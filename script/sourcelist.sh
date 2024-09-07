#! /bin/sh
set -euC
hg manifest | \
    grep 'NeatLauncher' | \
    sed 's@^project/android/NeatLauncher/@@' | \
    grep -v longbrain
