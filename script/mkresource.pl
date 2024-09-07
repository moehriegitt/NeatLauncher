#! /usr/bin/perl

# Read resources and make a class encapsulating all the tedious getString(),
# R.id.... blahblah stuff.  The binders are awkward.  Using the stuff
# manually feels last millenium, and is not typesafe.

use strict;
use warnings;
use autodie;
use Carp;
use utf8;
use XML::LibXML;
use Data::Dumper;
use XML::LibXML;
use open qw(:std :utf8);

$Data::Dumper::Indent = 1;
$Data::Dumper::Sortkeys = 1;

sub read_file($)
{
    my ($fn) = @_;
    print STDERR "R $fn\n";
    open(my $f, '<', $fn);
    local $/ = undef;
    return <$f>;
}

sub write_file($$)
{
    my ($fn, $text) = @_;
    print STDERR "W $fn\n";
    croak "No text to save" unless defined $text;
    open(my $f, '>', "$fn.new");
    print { $f } $text;
    close ($f);
    rename ("$fn.new", $fn);
}

sub mkdir_p($);
sub mkdir_p($)
{
    my ($s) = @_;
    return if -d $s;
    die "ERROR: '$s' is not a directory" if -e $s;

    $s =~ s(/+)(/)g;
    $s =~ s(/+$)();
    if ($s =~ m(^(.+)/[^/]+$)) {
        mkdir_p($1);
    }
    print STDERR "D $s\n";
    mkdir $s, 0777;
}

######################################################################

sub base_dir()
{
    my $d = '.';
    while (-d $d) {
        return $d if (-d "$d/res") && (-d "$d/java");
        my $main = "$d/app/src/main";
        return $main if (-d "$main/res") && (-d "$main/java");
        $d .= "/..";
    }
    die "ERROR: Unable to find ./.../app/src/main/{res,java} dirs\n";
}

sub java_dir($);
sub java_dir($)
{
    my ($dir) = @_;
    my @e =
        map { "$dir/$_" }
        grep { $_ !~ m/^[.]/ }
        do {
            opendir(my $d, $dir);
            readdir($d);
        };
    my @d = grep { -d $_ } @e;
    my @j = grep { (-f $_) && ($_ =~ m([.](?:java|kt)$)) } @e;

    return $dir if @j > 0;
    return java_dir($d[0]) if @d == 1;
    return "$dir/res";
}

sub init_dirs($)
{
    my ($C) = @_;
    $C->{base_dir} = base_dir();
    $C->{res_dir} = "$C->{base_dir}/res";
    $C->{top_java_dir} = "$C->{base_dir}/java";
    $C->{java_dir} = java_dir($C->{top_java_dir});
    $C->{top_dir} = "$C->{base_dir}/../../..";
    $C->{meta_dir} = "$C->{top_dir}/metadata";
    $C->{meta_en} = "$C->{meta_dir}/en-US";

    unless (-d "$C->{meta_en}") {
        die "ERROR: '$C->{meta_en}' not found or no directory";
    }

    (my $subdir = $C->{java_dir}) =~ s(^\Q$C->{top_java_dir}/\E)() or
        die "ERROR: No subdir for Java/Kotlin files in '$C->{top_java_dir}'\n";
    ($C->{pack} = $subdir) =~ s(/)(.)g;
}

sub learn_res_node($$$$$)
{
    my ($C, $fn, $dom, $type, $def) = @_;
    my $name = $def->getAttribute('name') or die "$fn: ERROR: $type node without name\n";
    my $e = ($C->{resource}{$type}{$name} //= { type => $type, name => $name });
    my $attrs = { map { ($_->nodeName => $_->value) } $def->attributes };
    delete $attrs->{name};
    while (my ($k,$v) = each %$attrs) {
        $e->{attr}{$k}{$fn} = $v;
    }

    my $text = '';
    my $child_i = 0;
    for my $child ($def->childNodes) {
        my $kind = $child->nodeType;
        if ($kind == XML_COMMENT_NODE) {
            # ignore
        }
        elsif ($kind == XML_TEXT_NODE) {
            $text .= $child->data;
        }
        elsif ($kind == XML_ELEMENT_NODE) {
            my $type2 = $child->nodeName;
            my $g = ($e->{child}{$type2}[$child_i++]{$fn} //= { type => $type2 });
            for my $a ($child->attributes) {
                $g->{attr}{$a->nodeName} = $a->value;
            }
            my $text2 = '';
            for my $child2 ($child->childNodes) {
                my $kind2 = $child2->nodeType;
                if ($kind2 == XML_COMMENT_NODE) {
                }
                elsif ($kind2 == XML_TEXT_NODE) {
                    $text2 .= $child2->data;
                }
                else {
                    die "$fn: ERROR: Unrecognised child node of $type.$type2: type='$kind2'\n";
                }
            }
            $g->{text} = $text2;
        }
        else {
            die "$fn: ERROR: Unrecognised child node of $type: type='$kind'\n";
        }
    }

    $e->{text}{$fn} = $text;
}

{
    my %learn_cb = (
        attr         => \&learn_res_node,
        string       => \&learn_res_node,
        dimen        => \&learn_res_node,
        color        => \&learn_res_node,
        style        => \&learn_res_node,
        array        => \&learn_res_node,
        string_array => \&learn_res_node,
    );

    sub learn_res_resources($$$)
    {
        my ($C, $fn, $dom) = @_;
        my ($root) = $dom->childNodes;
        for my $def ($root->childNodes) {
            if ($def->nodeType == XML_ELEMENT_NODE) {
                (my $type = $def->nodeName) =~ s(-)(_)g;
                if (my $cb = $learn_cb{$type}) {
                    $cb->($C, $fn, $dom, $type, $def);
                }
                else {
                    print STDERR "$fn: WARNING: Unrecognised resource type: '$type'\n";
                }
            }
        }
    }
}

sub learn_res_layout($$$$)
{
    my ($C, $fn, $name, $dom) = @_;
    # print STDERR "LAYOUT '$name'\n";
}

sub learn_res_xml($$)
{
    my ($C, $fn) = @_;
    print STDERR "R $fn (XML)\n";
    my $dom = XML::LibXML->load_xml(location => $fn);
    my ($root) = $dom->childNodes;
    my $rootName = $root->nodeName;
    if ($rootName eq 'resources') {
        return learn_res_resources($C, $fn, $dom);
    }
    if (($rootName eq 'adaptive-icon') ||
        ($rootName eq 'vector'))
    {
        # graphics are currently ignored
        return;
    }
    if ($fn =~ m(res/layout/([^/]+)[.]xml$)) {
        return learn_res_layout($C, $fn, $1, $dom);
    }
    if ($fn =~ m(res/xml/([^/]+)[.]xml$)) {
        # current, ignore these
        return;
    }
    die "ERROR: Unknown tag: $fn : ".$root->nodeName;
}

sub learn_res_rec($$);
sub learn_res_rec($$)
{
    my ($C, $dir) = @_;
    for my $e (do { opendir(my $d, $dir); readdir($d) }) {
        next if $e =~ m(^[.]);
        my $full = "$dir/$e";
        if (-d $full) {
            learn_res_rec($C, $full);
        }
        elsif ((-f $full) && ($e =~ m([.]xml$))) {
            learn_res_xml($C, $full);
        }
    }
}

sub learn_resources($)
{
    my ($C) = @_;
    learn_res_rec($C, $C->{res_dir});
}

######################################################################

sub has_value($$)
{
   my ($m, $k) = @_;
   return (grep { $_ eq $k } values %$m) > 0;
}

sub make_enums($$)
{
    my ($C, $M) = @_;
    my $R = $C->{resource};
    for my $k (
        sort
        grep { has_value($_->{attr}{translatable}, 'false') }
        values %{ $R->{string_array} })
    {
        my $name = $k->{name};
        (my $pref = $name) =~ s((_choice|_key)+$)();
        # print "DEBUG: string_array non-translatable '$k->{name}' -> '$pref'\n";
        my @item = map { my ($v) = values %$_; $v->{text} } @{ $k->{child}{item} // [] };
        for my $i (0..$#item) {
            my $v = $item[$i];
            (my $q = $v) =~ s([^0-9a-z]+)(_)gi;
            $q =~ s(^_+)();
            $q =~ s(_+$)();
            # print "DEBUG: '$name' '$pref' $i '$v' '$q'\n";
            push @{ $M->{const} }, {
                order => "$name $i",
                value => $i,
                enum => $name,
                key => "${pref}_$q",
            };
        }
    }
}

######################################################################

sub lexnum_cmp($$)
{
    my ($x, $y) = @_;
    my @x = split /(\d+)/, $x;
    my @y = split /(\d+)/, $y;
    while (@x && @y) {
        my $x0 = ($x[0] =~ /^\d/) ? (shift @x) : '';
        my $y0 = ($y[0] =~ /^\d/) ? (shift @y) : '';
        if (($x0 ne '') || ($y0 ne '')) {
            return ($x0 <=> $y0) || ($x0 cmp $y0) || next;
        }
        $x0 = shift @x;
        $y0 = shift @y;
        (my $x1 = $x0) =~ s([^a-z])()gi;
        (my $y1 = $y0) =~ s([^a-z])()gi;
        return (lc($x1) cmp lc($y1)) || ($x1 cmp $y1) || ($x0 cmp $y0) || next;
    }
    return @x <=> @y;
}

sub make_kotlin($$)
{
    my ($C, $M) = @_;

    my $t = "package $C->{pack}\n\n";
    for my $c (sort { lexnum_cmp($a->{order}, $b->{order}) } @{ $M->{const} }) {
        my $k = $c->{key};
        my $v = $c->{value};
        $t .= "const val $k = $v\n";
    }
    return $t;
}

######################################################################

sub idx($@)
{
    my $x = shift;
    for my $i (@_) {
        return unless defined($x) && defined($i);
        if (ref($x) eq 'HASH') {
            $x = $x->{$i};
        }
        elsif (ref($x) eq 'ARRAY') {
            $x = $x->[$i];
        }
        else {
            die "ERROR: Cannot index '$x'" unless ref($x);
        }
    }
    return $x;
}

sub max(@)
{
    my $r = shift;
    for my $s (@_) {
        $r = $s if $r < $s;
    }
    return $r;
}

sub trim($)
{
    my ($s) = @_;
    confess unless defined $s;
    $s =~ s(^\s+)()s;
    $s =~ s(\s+$)()s;
    return $s;
}

sub normalize_space($)
{
    my ($s) = @_;
    confess unless defined $s;
    $s =~ s(\s+)( )gs;
    return trim($s);
}

sub normalize_lines($)
{
    my ($s) = @_;
    confess unless defined $s;
    my @s = map { normalize_space($_)."\n" } split /\n/, $s;
    $s = join('', @s);
    $s =~ s(\n\n+)(\n\n)g;
    return trim($s);
}

sub text_unwrap($)
{
    my ($s) = @_;
    confess unless defined $s;
    my @s = map { normalize_space($_)."\n" } split /\n\n+/, $s;
    $s = join("\n", @s);
    return $s;
}

sub text_list($)
{
    my ($s) = @_;
    confess unless defined $s;
    my @s = map { "  $_\n" } split /\n/, $s;
    $s = join('', @s);
    return $s;
}

sub make_grid($$)
{
    my ($C, $G) = @_;

    my $grid_min  = idx($C, qw(resource string_array time_grid_min    child item)) // [];
    my $grid_hour = idx($C, qw(resource string_array time_grid_hour   child item)) // [];
    my $word_min  = idx($C, qw(resource string_array time_word_min    child item)) // [];
    my $word_hour = idx($C, qw(resource string_array time_word_hour   child item)) // [];
    my $grid_exc  = idx($C, qw(resource string_array time_grid_except child item)) // [];
    my $word_exc  = idx($C, qw(resource string_array time_word_except child item)) // [];

    my $dirs = {};
    for my $arr ($grid_min, $grid_hour, $word_min, $word_hour) {
        for my $item (@$arr) {
            for my $dir (keys %$item) {
                $dirs->{$dir} = 1;
            }
        }
    }
    for my $dir (sort keys %$dirs) {
        my @exc  = map { idx($_, $dir, 'text') } @$grid_exc;
        while (@exc && !defined($exc[-1])) { pop @exc; }
        unless (@exc) {
            @exc  = map { idx($_, $dir, 'text') } @$word_exc;
            while (@exc && !defined($exc[-1])) { pop @exc; }
        }

        my $min_cnt = max(scalar(@$grid_min), scalar(@$word_min));
        my @min =
            map {
                idx($grid_min, $_, $dir, 'text') //
                idx($word_min, $_, $dir, 'text')
            }
            0..$min_cnt-1;
        while (@min && !defined($min[-1])) { pop @min; }
        $min_cnt = scalar(@min);
        my $min_gap = 60.0 / $min_cnt;

        my $hour_cnt = max(scalar(@$grid_hour), scalar(@$word_hour));
        my @hour =
            map {
                idx($grid_hour, $_, $dir, 'text') //
                idx($word_hour, $_, $dir, 'text')
            }
            0..$hour_cnt-1;
        while (@hour && !defined($hour[-1])) { pop @hour; }
        $hour_cnt = scalar(@hour);

        my $seqs = {};
        for my $hour (0..23) {
            for my $min (0..59) {
                my $mrnd = int(int($min / $min_gap) * $min_gap);
                my $time = sprintf "%02d:%02d", $hour, $mrnd;
                my ($str) = grep { $_ =~ m(\b\Q$time\E\b) } @exc;
                $str //= '';
                $str =~ s(\b\Q$time\E\b)( )g;
                $str = normalize_space($str);
                if ($str ne '') {
                    $seqs->{$str} = 1;
                    printf "TIME: $dir: %02d:%02d $str\n", $hour, $mrnd;
                }
                else {
                    $str = $min[($min * @min) / 60];
                    $str =~ s([-+=])(' '.$hour[$hour % 12].' ')e;
                    $str =~ s((\d)\s+)($1)g;
                    $str = normalize_space($str);
                    $seqs->{$str} = 1;
                    printf "TIME: $dir: %02d:%02d: $str\n", $hour, $mrnd;
                }
            }
        }

        my $words = {};
        for my $seq (keys %$seqs) {
            my $pre = undef;
            for my $word (split /\s+/, $seq) {
                my $e = ($words->{$word} //= { word => $word });
                if ($pre) {
                    $e->{pre}{$pre} = 1;
                }
                $pre = $word;
            }
        }
    }
}

######################################################################

sub get_string($$)
{
    my ($C, $k) = @_;
    my $S = $C->{resource}{string};
    return $S->{$k}{text} // die "ERROR: README/Metadata: Missing string: '$k'\n";
}

sub make_metadata($$)
{
    my ($C, $R) = @_;

    # read doc strings
    my $title = get_string($C, 'app_name');
    my $short_desc = get_string($C, 'readme_short_description');
    my $desc_head = get_string($C, 'readme_description');
    my $full_desc = get_string($C, 'readme_full_description');
    my $feat_head = get_string($C, 'readme_features');
    my $feat_list = get_string($C, 'readme_feature_list');
    my $shot_head = get_string($C, 'readme_screenshots');

    # learn languages
    my %lang = ();
    for my $s ($title, $short_desc, $desc_head, $full_desc, $feat_head, $feat_list) {
        for my $k (sort keys %$s) {
            if ($k =~ m(/values(?:-([^/]+))?/)) {
                my $l = $1 // 'en-US';
                $lang{$l} = { key => $k };
            }
        }
    }
    die "ERROR: No default language string found (for 'en-US' metadata)"
        unless $lang{'en-US'};

    # generate metadata and prepare readme
    my $readme = '';
    my $lang_en_key = undef;
    for my $lang (sort keys %lang) {
        my $k = $lang{$lang}{key};

        my $_title = normalize_space($title->{$k});
        my $_short = normalize_space($short_desc->{$k});
        my $_desc  = normalize_space($desc_head->{$k});
        my $_full  = normalize_lines($full_desc->{$k});
        my $_feat  = normalize_space($feat_head->{$k});
        my $_list  = normalize_lines($feat_list->{$k});
        my $_list2 = text_list($_list);

        mkdir_p("$C->{meta_dir}/$lang");
        write_file("$C->{meta_dir}/$lang/title.txt", "$_title\n");
        write_file("$C->{meta_dir}/$lang/short_description.txt", "$_short\n");

        my $_full2 = text_unwrap($_full);
        my $_full3 = "${_full2}\n\n${_feat}:\n\n${_list2}\n";
        $_full3 =~ s(\n\n+)(\n\n)g;
        write_file("$C->{meta_dir}/$lang/full_description.txt", $_full3);

        if ($lang eq 'en-US') {
            $lang_en_key = $k;
            $readme = "# $_title\n\n";
            $readme .= "$_short\n\n";
            $readme .= "## $_desc\n\n";
            $readme .= "$_full\n\n";
            $readme .= "## $_feat\n\n";
            $readme .= "$_list2\n\n";

            $readme =~ s(\n\n+)(\n\n)g;
            $readme =~ s(\n+$)();
            $readme .= "\n";
        }
    }

    # find screenshot images
    my @img = ();
    my $dir = "$C->{meta_en}/images/phoneScreenshots";
    for my $i (sort do { opendir(my $d, $dir); readdir($d); }) {
        next if $i =~ m(^[.]);
        next unless $i =~ m([.](?:png|jpg)$);
        next unless -f "$dir/$i";
        (my $rel = "$dir/$i") =~ s(^\Q$C->{top_dir}\E/?)();
        push @img, $rel;
    }

    # add images to README
    if (@img) {
        my $_shot = normalize_space($shot_head->{$lang_en_key});
        $readme .= "\n## $_shot\n\n";
        for my $img (@img) {
            $readme .= "!($img)\n";
        }
    }

    # write readme
    write_file("$C->{top_dir}/README.md", $readme);
}

######################################################################

my $C = {};
init_dirs($C);

learn_resources($C);

# print Dumper($C);

# Time Grid (not done yet)
#my $G = {};
#make_grid($C, $G);

# Enums
my $M = {};
make_enums($C, $M);
my $t = make_kotlin($C, $M);
write_file(
    "$C->{java_dir}/AutoResource.kt",
    "// Autogenerated by mkresource.pl, do not edit\n".$t);

# Metadata description and Readme
my $R = {};
make_metadata($C, $R)
