VERSION=`cat VERSION`

echo "Tagging all c3kit libraries with version $VERSION"

echo "Tagging APRON ----------------"
pushd apron
git tag $VERSION
git push --tags
popd

echo "Tagging SCAFFOLD ----------------"
pushd scaffold
git tag $VERSION
git push --tags
popd

echo "Tagging BUCKET ----------------"
pushd bucket
git tag $VERSION
git push --tags
popd

echo "Tagging WIRE ----------------"
pushd wire
git tag $VERSION
git push --tags
popd


