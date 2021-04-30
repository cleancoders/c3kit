echo "Deploying APRON ----------------"
pushd apron
lein deploy clojars
popd

echo "Deploying SCAFFOLD ----------------"
pushd scaffold
lein deploy clojars
popd

echo "Deploying BUCKET ----------------"
pushd bucket
lein deploy clojars
popd

echo "Deploying WIRE ----------------"
pushd wire
lein deploy clojars
popd


