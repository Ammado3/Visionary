#!/usr/bin/env zsh
set -e

DEVICE_ID="${1:-D10F50F2-B155-5933-BBE6-11138C13737D}"
BUNDLE_ID="com.o3.vanguard.visionary.VisionaryWMV5PKVDHD"

echo "==> Building Kotlin Framework for iOS Arm64..."
./gradlew :composeApp:linkDebugFrameworkIosArm64

echo "==> Building Xcode iOS project for device $DEVICE_ID..."
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination "id=$DEVICE_ID" -allowProvisioningUpdates build

APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData -name "Visionary.app" -path "*/Debug-iphoneos/*" 2>/dev/null | head -n 1)

if [ -z "$APP_PATH" ]; then
    echo "Error: Could not locate built Visionary.app in DerivedData"
    exit 1
fi

echo "==> Installing Visionary.app on device..."
xcrun devicectl device install app --device "$DEVICE_ID" "$APP_PATH"

echo "==> Launching Visionary on device..."
xcrun devicectl device process launch --device "$DEVICE_ID" "$BUNDLE_ID"

echo "==> Visionary running successfully!"
