#! /bin/sh

if [ -z $1 ];
  then echo "ERROR: A commit message is required.";
  exit 1;
fi

echo "Committing and pushing (to origin master) each library with commit message: $1"

echo "Installing APRON ----------------"
pushd apron
git commit -am "$1"
git push origin master
popd

echo "Installing SCAFFOLD ----------------"
pushd scaffold
git commit -am "$1"
git push origin master
popd

echo "Installing BUCKET ----------------"
pushd bucket
git commit -am "$1"
git push origin master
popd

echo "Installing WIRE ----------------"
pushd wire
git commit -am "$1"
git push origin master
popd

git commit -am "$1"
git push origin master
