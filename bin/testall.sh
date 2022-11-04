echo "Testing APRON ----------------"
pushd apron
clj -M:test:spec
clj -M:test:cljs once
popd

echo "Testing SCAFFOLD ----------------"
pushd scaffold
clj -M:test:spec
clj -M:test:cljs once
clj -M:test:css once
popd

echo "Testing BUCKET ----------------"
pushd bucket
clj -M:test:spec
clj -M:test:cljs once
popd

echo "Testing WIRE ----------------"
pushd wire
clj -M:test:spec
clj -M:test:cljs once
popd


