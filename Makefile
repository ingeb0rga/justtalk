SHELL := /bin/bash

-include .env
APK        := app/build/outputs/apk/debug/app-debug.apk
ADB        ?= adb

.PHONY: build test adb-install push-model clean

build:
	./gradlew assembleDebug
	@echo "APK: $(APK)"

test:
	./gradlew testDebugUnitTest

adb-install: build
	$(ADB) install -r $(APK)

## Push a model to the phone's internal storage (usage: make push-model MODEL=/path/to/model-dir)
push-model:
	@test -n "$(MODEL)" || (echo "Usage: make push-model MODEL=/path/to/model-dir" && exit 1)
	$(ADB) push $(MODEL)/ /data/local/tmp/$(notdir $(MODEL))/
	$(ADB) shell "run-as com.kafkasl.phonewhisper mkdir -p files/models/$(notdir $(MODEL))"
	@for f in $$(ls $(MODEL)/*.onnx $(MODEL)/*.ort $(MODEL)/*.txt 2>/dev/null); do \
		echo "  copying $$(basename $$f)..."; \
		$(ADB) shell "run-as com.kafkasl.phonewhisper cp /data/local/tmp/$(notdir $(MODEL))/$$(basename $$f) files/models/$(notdir $(MODEL))/$$(basename $$f)"; \
	done
	@echo "Model pushed: $(notdir $(MODEL))"

clean:
	./gradlew clean
