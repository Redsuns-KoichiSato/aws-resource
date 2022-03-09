build-libs:
	(cd listFunction && mvn clean install)
	find "$(ARTIFACTS_DIR)/.." -name lib -exec rm -rf {} +
	mkdir -p "$(ARTIFACTS_DIR)/java/lib/"
	mv listFunction/target/PollyNotes*.jar "$(ARTIFACTS_DIR)/java/lib/PollyNotes-lib.jar"
	rm -rf listFunction/target/*