#! /bin/sh

if [ -z $1 ];
  then echo "ERROR: A commit message is required.";
  exit 1;
fi

echo "Committing and pushing (to origin master) each library with commit message: $1"

echo "Committing & Pushing APRON ----------------"
pushd apron
git commit -am "$1"
git push origin master
popd

echo "Committing & Pushing SCAFFOLD ----------------"
pushd scaffold
git commit -am "$1"
git push origin master
popd

echo "Committing & Pushing BUCKET ----------------"
pushd bucket
git commit -am "$1"
git push origin master
popd

echo "Committing & Pushing WIRE ----------------"
pushd wire
git commit -am "$1"
git push origin master
popd


echo "Committing & Pushing c3kit ----------------"
git commit -am "$1"
git push origin master
