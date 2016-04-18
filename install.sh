#! /bin/bash

# Set ROOT to empty if missing:
if [ -z "${ROOT+xxx}" ]; then
  ROOT=
fi

function cleanClone {
  GIT_DIR=$ROOT$HOME/git
  mkdir -p $GIT_DIR
  
  pushd $GIT_DIR
  echo Cloning into $GIT_DIR/$2
  rm -rf $2
  git clone https://github.com/$1/$2.git
  popd
}

function replaceLineContaining {
  FILE=$ROOT$1
  sed -i 's/.*$2.*/$3/' $FILE
}

function addIfMissing {
  if [ "`grep \"$2\" $1`" = "" ]; then
  	FILE=$ROOT$1
  	DIR=`dirname $FILE`
    echo Adding $2 to $FILE
    mkdir -p $DIR
    echo "$2" >> $FILE
  else 
  	echo Not adding $2 to $1
  fi
}

function ensurePackage {
  sudo DEBIAN_FRONTEND=noninteractive apt-get -y install $1
}

addIfMissing /etc/X11/xinit/xinitrc "xset s off         # don't activate screensaver"
addIfMissing /etc/X11/xinit/xinitrc "xset -dpms         # disable DPMS (Energy Star) features."
addIfMissing /etc/X11/xinit/xinitrc "xset s noblank     # don't blank the video device"

replaceLineContaining /etc/lightdm/lightdm.conf "xserver-command=X" "xserver-command=X -s 0 dpms"
replaceLineContaining /etc/X11/xinit/xserverrc "exec \\/usr\\/bin\\/X" "exec \\/usr\\/bin\\/X -s 0 dpms -nolisten tcp \"$@\""

sudo apt-get update

ensurePackage git
ensurePackage wget
ensurePackage chromium
ensurePackage x11-xserver-utils
ensurePackage java
# Required for I2C:
# ensurePackage python-smbus
# ensurePackage i2c-tools

cleanClone pirkkajokela theradiator
