package de.theiling.neatlauncher

fun selectTheme(fontChoice: Int, colorChoice: Int) =
    when(fontChoice) {
        font_sans -> when (colorChoice) {
            color_ambr -> R.style.AppTheme_Sans_Amber
            color_pump -> R.style.AppTheme_Sans_Pumpkin
            color_redx -> R.style.AppTheme_Sans_Red
            color_lila -> R.style.AppTheme_Sans_Lilac
            color_blue -> R.style.AppTheme_Sans_Blue
            color_cyan -> R.style.AppTheme_Sans_Cyan
            else       -> R.style.AppTheme_Sans_Achromic
        }
        font_seri -> when (colorChoice) {
            color_ambr -> R.style.AppTheme_Serif_Amber
            color_pump -> R.style.AppTheme_Serif_Pumpkin
            color_redx -> R.style.AppTheme_Serif_Red
            color_lila -> R.style.AppTheme_Serif_Lilac
            color_blue -> R.style.AppTheme_Serif_Blue
            color_cyan -> R.style.AppTheme_Serif_Cyan
            else       -> R.style.AppTheme_Serif_Achromic
        }
        font_mono -> when (colorChoice) {
            color_ambr -> R.style.AppTheme_Mono_Amber
            color_pump -> R.style.AppTheme_Mono_Pumpkin
            color_redx -> R.style.AppTheme_Mono_Red
            color_lila -> R.style.AppTheme_Mono_Lilac
            color_blue -> R.style.AppTheme_Mono_Blue
            color_cyan -> R.style.AppTheme_Mono_Cyan
            else       -> R.style.AppTheme_Mono_Achromic
        }
        font_curs -> when (colorChoice) {
            color_ambr -> R.style.AppTheme_Cursive_Amber
            color_pump -> R.style.AppTheme_Cursive_Pumpkin
            color_redx -> R.style.AppTheme_Cursive_Red
            color_lila -> R.style.AppTheme_Cursive_Lilac
            color_blue -> R.style.AppTheme_Cursive_Blue
            color_cyan -> R.style.AppTheme_Cursive_Cyan
            else       -> R.style.AppTheme_Cursive_Achromic
        }
        else -> when (colorChoice) {
            color_ambr -> R.style.AppTheme_Ubuntu_Amber
            color_pump -> R.style.AppTheme_Ubuntu_Pumpkin
            color_redx -> R.style.AppTheme_Ubuntu_Red
            color_lila -> R.style.AppTheme_Ubuntu_Lilac
            color_blue -> R.style.AppTheme_Ubuntu_Blue
            color_cyan -> R.style.AppTheme_Ubuntu_Cyan
            else       -> R.style.AppTheme_Ubuntu_Achromic
        }
    }
