TEST_CERT_DIR="$1"

cd $TEST_CERT_DIR || exit

#openssl req -x509 -nodes -sha256 -newkey rsa:4096 -keyout test_key.pem -out test_cert.pem -days 365
#openssl pkcs12 -export -out test.p12 -inkey test_key.pem -in test_cert.pem

keytool -genkey -ext SAN=IP:0.0.0.0 -keyalg RSA -alias test -keystore ks.p12 -keypass testpass -storepass testpass -validity 365 -keysize 2048 -deststoretype pkcs12
