echo "Installing APRON ----------------"
pushd apron
lein install
popd

echo "Installing SCAFFOLD ----------------"
pushd scaffold
lein install
popd

echo "Installing BUCKET ----------------"
pushd bucket
lein install
popd

echo "Installing WIRE ----------------"
pushd wire
lein install
popd


