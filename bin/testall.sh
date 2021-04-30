echo "Testing APRON ----------------"
pushd apron
lein spec
lein cljs once
popd

echo "Testing SCAFFOLD ----------------"
pushd scaffold
lein spec
lein cljs once
lein css once
popd

echo "Testing BUCKET ----------------"
pushd bucket
lein spec
lein cljs once
popd

echo "Testing WIRE ----------------"
pushd wire
lein spec
lein cljs once
popd


