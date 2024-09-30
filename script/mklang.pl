#! /usr/bin/perl

# Read a mail with translations sent from the translation.html form and
# patch the resources to include the updates.

use strict;
use warnings;
use autodie;
use utf8;
use open qw(:std :utf8);
use Carp;
use Data::Dumper;
use XML::LibXML;
use MIME::Base64;
use Text::Diff;

$Data::Dumper::Indent = 1;
$Data::Dumper::Sortkeys = 1;

my $ind = "    ";
my $cr1 = "\n$ind";
my $cr2 = "\n$ind$ind";

my $width = 90;
my $short = 70;

######################################################################

my $in_place = 0;

######################################################################

sub init_config0($$)
{
    my ($top, $main) = @_;
    my $res = "$main/res";
    return {
        top => $top,
        main => $main,
        res => $res,
        values => "$res/values",
    };
}

sub init_config()
{
    my $dir = ".";
    for (1..10) {
        die "ERROR: No source dir found" unless -d "$dir";
        if ((-d "$dir/app/src/main") && (-f "$dir/build.gradle.kts")) {
            return init_config0("$dir", "$dir/app/src/main");
        }
        if ((-d "$dir/java") && (-f "$dir/res/values/strings.xml")) {
            return init_config0("$dir/../../..", "$dir");
        }
        $dir = "$dir/..";
    }
}

######################################################################

sub line_rewrap($$)
{
    my ($ss, $w) = @_;
    my @l = ();
    my $l = '';
    for my $s (split /\s+/, $ss) {
        if ($s eq '') {}
        elsif ($l eq '') { $l = $s; }
        elsif ((length($l) + 1 + length($s)) > $w) {
            push @l, $l;
            $l = '';
            redo;
        }
        else {
            $l .= " $s";
        }
    }
    push @l, $l if $l ne '';
    return join("\n", @l);
}

sub text_rewrap($$)
{
    my ($s, $w) = @_;
    if ($s =~ m(\n\n)) {
        return join("\n\n", map { line_rewrap($_, $short - $w) } split /\n{2,}/, $s);
    }
    if ($s =~ m(^- )) {
        $s =~ s(\n(?!- ))( )g;
        return join("\n", map { line_rewrap($_, $width - $w) } split /\n/, $s);
    }
    return $s;
}

sub text_indent($$)
{
    my ($s, $w) = @_;
    my $i = $ind x $w;
    $s =~ s(\n)(\n$i)g;
    $s =~ s( +\n)(\n)g;
    $s =~ s(\s+$)();
    return $s;
}

######################################################################

sub dir_file_of($)
{
    my ($s) = @_;
    $s =~ s(/+)(/)g;
    $s =~ s(/$)();
    return unless $s =~ m(^(.*/)([^/]+)$);
    return ($1, $2);
}

sub dir_of($)
{
    my ($s) = @_;
    my ($dir, $file) = dir_file_of($s);
    return $dir;
}

sub file_of($)
{
    my ($s) = @_;
    my ($dir, $file) = dir_file_of($s);
    return $file;
}

sub try_load_file($)
{
    my ($fn) = @_;
    no autodie;
    open(my $f, '<', $fn) or return;
    local $/ = undef;
    my $s = <$f>;
    return $s;
}

sub mkdir_p($);
sub mkdir_p($)
{
    my ($s) = @_;
    return unless defined $s;
    return if -d $s;
    mkdir_p(dir_of($s));
    mkdir $s, 0777;
}

sub load_file($)
{
    my ($fn) = @_;
    return try_load_file($fn) // die "ERROR: Unable to open '$fn' for reading\n";
}

sub save_file($$)
{
    my ($fn, $data) = @_;
    mkdir_p(dir_of($fn));
    open(my $g, '>', "$fn.new");
    print { $g } $data;
    close $g;
    # rename ("$fn.new", $fn);
}

sub trim($)
{
    my ($s) = @_;
    return unless defined $s;
    $s =~ s/^\s+//;
    $s =~ s/\s+$//;
    return $s;
}

sub normalize_space($)
{
    my ($s) = @_;
    return unless defined $s;
    $s =~ s/\s+/ /g;
    return trim($s);
}

sub url_unquote($)
{
    my ($s) = @_;
    $s =~ s/[+]/ /g;
    $s =~ s/%([0-9A-Fa-f]{2})/chr(hex($1))/eg;
    $s =~ s/\r\n/\n/g;
    utf8::decode($s) or die "ERROR: '$s' is not UTF-8 encoded";
    return $s;
}

sub normalize_lang($)
{
    my ($s) = @_;

    $s = trim($s);

    # new locale (will be handled separately)
    if ($s eq '*') {
        return $s;
    }

    # normalise the format:
    if ($s eq '') { }
    elsif ($s eq '_') {
        $s = '';
    }
    elsif ($s =~ m(^([a-z]{2,4})$)i) {
        $s = lc($1);
    }
    elsif ($s =~ m(^([a-zA-Z]{2,4})[-_.,+]r([a-zA-Z]{2})$)) {
        $s = lc($1).'-r'.uc($2); # reformat if already with 'r'
    }
    elsif ($s =~ m(^([a-z]{2,4})[-_.,+]([A-Z]{2})$)i) {
        $s = lc($1).'-r'.uc($2); # plus region
    }
    else {
        die "ERROR: Unrecognised locale syntax: '$s'\n";
    }

    # normalise the Android default language (en-US) to '':
    if ($s eq 'en-rUS') {
        $s = '';
    }
    elsif ($s eq 'en') {
        $s = '';
    }

    return $s;
}

sub normalize_path($)
{
    my ($s) = @_;
    while ($s =~ s(/[.]/)(/)g) {}
    return $s;
}

######################################################################

sub parse_submission($)
{
    my ($fn) = @_;
    my $s = load_file($fn);
    $s =~ s(\r\n)(\n)g;
    $s =~ s(^\s+)();
    $s =~ s(\s+$)();
    my ($head, $body) = split /\n\n/, $s, 2;
    $head =~ s(\n[ \t]+)( )g;
    my %head = (
        map {
            my ($k, $v) = split /:/, $_, 2;
            $k =~ s/-/_/g;
            (lc($k) => normalize_space($v));
        }
        split /\n/, $head
    );

    my $data = undef;
    my $te = lc($head{content_transfer_encoding} // 'none');
    if ($te eq 'base64') {
        $data = decode_base64($body);
    }
    elsif ($te eq 'none') {
        $data = $body;
    }
    else {
        die "ERROR: Unrecognised Content-Transfer-Encodig: $te\n";
    }

    $data = trim($data);
    my @form = split /[&]/, $data;
    @form = split /[;]/, $data if @form <= 1;
    my %form = ();
    $form{meta}{From} = $head{from} // '';
    $form{meta}{Date} = $head{date} // '';
    $form{meta}{Subject} = $head{subject} // '';
    $form{meta}{'Reply-To'} = $head{reply_to} // '';
    for my $kv (@form) {
        my ($k, $v) = split /=/, $kv, 2;
        my $val = url_unquote($v);
        if ($k eq 'lang_new') {
            $form{lang_new} = normalize_lang($val);
            next;
        }
        if ($k =~ m(^meta_(.+)$)) {
            $form{meta}{"X-".ucfirst($1)} = $val;
            next;
        }
        my ($key, $idx, $lang) = split /[.]/, $k, 3;
        ($idx, $lang) = ('', $idx) unless defined $lang;
        die "ERROR: Illegal key: '$k', expected NAME.INDEX.LANG\n" unless defined $lang;
        $lang = normalize_lang($lang);

        my $type = '';
        if ($key =~ m(^(.+)([-*])$)) {
            $key = $1;
            $type = $2;
        }

        my $e = ($form{strings}{$lang}{$key} //= { key => $key, type => $type });
        if ($idx eq '') {
            if ($type eq '*') {
                $e->{rdata} = [ split /\n/, $val ];
            } else {
                $e->{rdata} = $val;
            }
        } else {
            $e->{rdata}{$idx} = $val;
        }
    }

    my $x = delete $form{strings}{'*'};
    if (my $new = $form{lang_new}) {
        $form{strings}{$new} = $x;
    }

    return \%form;
}

######################################################################

sub xml_node_qtext($)
{
    my ($r) = @_;
    my $t = '';
    for my $n ($r->childNodes) {
        my $type = $n->nodeType;
        if ($type == XML_TEXT_NODE) {
            $t .= "$n"; # quoted string, no need to quote_html() later
        }
        elsif ($type == XML_COMMENT_NODE) {}
        else {
            die "ERROR: Unrecognised XML node type: $type\n";
        }
    }

    # unindent and trim, but no other normalization shenanegans:
    if ($t =~ m(\n?([ ]+))) {
        $t =~ s(^\Q$1\E)()gm;
    }
    return trim($t);
}

sub load_strings($$)
{
    my ($C, $lang) = @_;

    # already exists, nothing to be loaded:
    return if $C->{strings}{$lang};

    # read file:
    my $sd = ($lang eq '') ? "values" : "values-$lang";
    my $fn = "$C->{res}/$sd/strings.xml";
    my $e = $C->{strings}{$lang} = { lang => $lang, file => $fn, map => {}, arr => [] };
    my $s = ($e->{orig} = try_load_file($fn)) // return;

    # parse XML:
    my $dom = XML::LibXML->load_xml(location => $fn);
    my ($root) = $dom->childNodes;
    die "ERROR: $fn: not a resource file\n" unless $root->nodeName eq 'resources';
    my $pre = {};
    for my $n ($root->childNodes) {
        my $type = $n->nodeType;
        if ($type == XML_COMMENT_NODE) {
            push @{ $pre->{com} }, "$n"; # raw XML, can be printed as is.
            $pre->{nl}--; # a XML usually comes with an extra newline
        }
        elsif ($type == XML_TEXT_NODE) {
            my $s = "$n";
            die "ERROR: $fn: expected only white space between elements, found: '$s'"
                unless $s =~ m(^\s*$);
            $pre->{nl} += $s =~ tr(\n)(\n); # count newlines, add them.
        }
        elsif ($type == XML_ELEMENT_NODE) {
            my $tag = $n->nodeName;
            my $q = { tag => $tag, com => delete $pre->{com}, nl => delete $pre->{nl} };
            my %attr = ();
            for my $a ($n->attributes) {
                my ($k, $v) = ($a->nodeName, $a->value);
                $k =~ s(^.*:)();
                $attr{$k} = $v;
            }
            $q->{name} = delete $attr{name};

            $q->{var_len} = ((delete $attr{varLen}) // 'false') eq 'true';

            $q->{if_empty} = (delete $attr{ifEmpty}) // 'error';
            $q->{if_empty} = '' if $q->{if_empty} eq 'error';

            die "ERROR: $fn: $q->{name}: Unrecognised attributes: ".Dumper(\%attr)
                if scalar(keys %attr);

            if ($tag eq 'string') {
                $q->{qdata} = xml_node_qtext($n);
            }
            elsif ($tag eq 'string-array') {
                $q->{qdata} = [];
                for my $c ($n->childNodes) {
                    my $type2 = $c->nodeType;
                    next if $type2 == XML_COMMENT_NODE;
                    next if $type2 == XML_TEXT_NODE;
                    if ($type2 == XML_ELEMENT_NODE) {
                        die "ERROR: $fn: '<item>' expected\n" unless $c->nodeName eq 'item';
                        push @{ $q->{qdata} }, xml_node_qtext($c);
                    }
                }
            }
            else {
                die "ERROR: $fn: Unrecognised element: $tag\n";
            }
            push @{ $e->{arr} }, $q;
            $e->{map}{$q->{name}} = $q;
        }
        else {
            die "ERROR: $fn: Unrecognised XML node type: $type\n";
        }
    }
}

sub ensure_strings($$)
{
    my ($C, $m) = @_;
    die unless $C->{strings}{''}{arr}; # load_all() is missing
    for my $lang (sort keys %{ $m->{strings} }) {
        load_strings($C, $lang);
    }
}

sub load_all($)
{
    my ($C) = @_;
    my $dir = $C->{res};
    for my $e (sort do { opendir(my $d, $dir); readdir($d) }) {
        my $full = "$dir/$e";
        next if $e =~ /^[.]/;
        next unless -d $full;
        next unless $e =~ /^values(?:-(.+))?\b/;
        my $lang = $1 // '';
        next unless -f "$full/strings.xml";
        load_strings($C, $lang);
    }
    $C->{names} = [ map { $_->{name} } @{ $C->{strings}{''}{arr} } ];
}

######################################################################

sub quote_html($)
{
    my ($s) = @_;
    $s =~ s(\&)(&amp;)g;
    $s =~ s(\")(&quot;)g;
    $s =~ s(<)(&lt;)g;
    $s =~ s(>)(&gt;)g;
    return $s;
}

sub block($)
{
    my ($s) = @_;
    $s =~ s(^\s+)();
    $s =~ s(^\s*\|)()mg;
    $s =~ s(\s+$)();
    return "$s\n";
}

sub gen_strings($$$)
{
    my ($C, $lang, $str) = @_;
    my $org = $C->{strings}{''};

    my $default = ($lang eq '');
    my $qlang = quote_html($lang);
    # print STDERR "DEBUG: lang=$lang, default=$default, file=$str->{file}\n";
    my $t = block qq{
        |<?xml version="1.0" encoding="utf-8"?>};
    if ($default) {
        $t .= block qq{
            |<resources
            |    xmlns:tools="http://schemas.android.com/tools">};
    } else {
        $t .= block qq{
            |<resources>};
    }
    for my $name (@{ $C->{names} }) {
        my $e = $str->{map}{$name} // next;
        my $d = $org->{map}{$name} // die;
        die unless $e->{tag} eq $d->{tag}; # string vs. string-array across langs??

        my $attr = '';
        if ($default) {
            $attr .= !$d->{var_len}  ? '' : qq( tools:varLen="true");
            $attr .= !$d->{if_empty} ? '' : qq( tools:ifEmpty="$e->{if_empty}");
        }

        my $qtxt = '';
        my $ind_txt = 2;
        if ($e->{tag} eq 'string') {
            $qtxt = $e->{qdata} // die;
            $ind_txt = 1;
        }
        elsif ($e->{tag} eq 'string-array') {
            $qtxt = join("\n", map { "<item>$_</item>" } @{ $e->{qdata} // die });
        }

        if (($qtxt eq '') && ($d->{if_empty} ne 'keep')) {
            next;
        }

        # maybe break lines:
        my $nl = ($qtxt =~ m(\n)) || ($e->{tag} eq 'string-array');
        unless ($nl) {
            my $s = qq(    <$e->{tag} name="$e->{name}"$attr>$qtxt</$e->{tag}>);
            $nl ||= (length($s) > $width);
        }
        if ($nl) {
            $qtxt = text_indent("\n$qtxt", $ind_txt)."\n    ";
        }

        # render element:
        if ($d->{nl} >= 2) {
            $t .= "\n";
        }
        if ($default) {
            for my $com (@{ $d->{com} // [] }) {
                $t .= block qq(
                    |    $com);
            }
        }
        $t .= block qq(
            |    <$e->{tag} name="$e->{name}"$attr>$qtxt</$e->{tag}>);
    }
    $t .= block qq{
        |</resources>};

    $str->{modi} = $t;
}

sub gen_all($)
{
    my ($C) = @_;
    for my $lang (sort keys %{ $C->{strings} }) {
        gen_strings($C, $lang, $C->{strings}{$lang});
    }
}

sub save_strings($$$)
{
    my ($C, $lang, $str) = @_;
    save_file($str->{file}, $str->{modi});
}

sub save_all($)
{
    my ($C) = @_;
    for my $lang (sort keys %{ $C->{strings} }) {
        save_strings($C, $lang, $C->{strings}{$lang});
    }
}

sub diff_file($$$)
{
    my ($fn, $orig, $modi) = @_;
    $orig //= '';
    return if $orig eq $modi;

    my $diff = diff(\$orig, \$modi,
        {
            STYLE => 'Unified',
            FILENAME_A => normalize_path("a/$fn"),
            MTIME_A => (stat($fn))[9] // 0,
            FILENAME_B => normalize_path("b/$fn"),
            MTIME_B => time(),
        });
    print "\n";
    print $diff;
}

sub diff_strings($$$)
{
    my ($C, $lang, $str) = @_;
    diff_file($str->{file}, $str->{orig}, $str->{modi});
}

sub diff_all($)
{
    my ($C) = @_;
    for my $lang (sort keys %{ $C->{strings} }) {
        diff_strings($C, $lang, $C->{strings}{$lang});
    }
}

######################################################################

sub get_string($$$$$)
{
    my ($C, $lang, $key, $org, $mod) = @_;
    my $rdata = $mod->{rdata};

    if ($org->{tag} eq 'string') {
        my $qdata = quote_html($rdata);
        die "ERROR: Wrong type for <string> '$key': ".Dumper($mod) if ref($qdata);
        if ($qdata =~ /\n/) {
            if ($mod->{type} eq '') {
                $qdata = text_rewrap($qdata, 4);
            }
            if ($mod->{type} eq '-') {
                $qdata =~ s($)(\\n)gm;
            }
        }
        return $qdata;
    }

    if ($org->{tag} eq 'string-array') {
        my @rdata = ();
        if (ref($rdata) eq 'ARRAY') {
            @rdata = @$rdata;
        }
        elsif (ref($rdata) eq 'HASH') {
            for my $k (sort { $a <=> $b } keys %$rdata) {
                $rdata[$k] = $rdata->{$k};
            }
        }
        elsif (!ref($rdata)) {
            @rdata = split /\n/, $rdata;
        }
        else {
            die "ERROR: Unexpected data for '$key': ".Dumper($rdata);
        }

        my $conc = join("", @rdata);
        return [] if $conc eq '';

        my $qdata = [ map { quote_html($_) } @rdata ];
        return $qdata;
    }

    die "ERROR: Unknown tag: $org->{tag}\n";
}

sub patch_strings($$$$$)
{
    my ($C, $lang, $org, $str, $mod) = @_;
    my $default = ($lang eq '');
    my $orgm = $org->{map};
    my $strm = $str->{map};
    for my $key (sort keys %$mod) {
        my $o = $orgm->{$key} // do {
            print STDERR "WARNING: '$key' is modified, but is not an orig. string, skipping.\n";
            next;
        };
        my $qdata = get_string($C, $lang, $key, $o, $mod->{$key});
        if (defined $qdata) {
            $strm->{$key}{name} = $key;
            $strm->{$key}{tag} = $o->{tag};
            $strm->{$key}{qdata} = $qdata;
        }
        else {
            die "ERROR: '$key': is dropped, but it is an orig. string.\n" if $default;
            delete $strm->{$key};
        }
    }
}

sub apply_patch($$)
{
    my ($C, $m) = @_;
    for my $lang (sort keys %{ $m->{strings} }) {
        patch_strings($C, $lang, $C->{strings}{''}, $C->{strings}{$lang}, $m->{strings}{$lang});
    }
}

######################################################################

my $C = init_config();

my @file = ();
for my $arg (@ARGV) {
    if ($arg =~ m(^-)) {
        if ($arg =~ m(^-+(i|in-?place)$)) {
            $in_place = 1;
        }
        else {
            die "ERROR: Unrecognised option: $arg\n";
        }
    }
    else {
        push @file, $arg;
    }
}

load_all($C);
for my $file (@file) {
    my $m = parse_submission($file);
    ensure_strings($C, $m); # be sure to have at least an empty file
    apply_patch($C, $m);
    print STDERR "\n# META #\nFile: $file\n";
    for my $k (sort keys %{ $m->{meta} }) {
        (my $v = $m->{meta}{$k}) =~ s(\n)(\n    )g;
        print STDERR "$k: $v\n";
    }
}
gen_all($C);
if ($in_place) {
    save_all($C);
}
else {
    diff_all($C);
}
