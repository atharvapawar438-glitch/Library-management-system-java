#!/usr/bin/env zsh
set -e

cd "$(dirname "$0")"

# Auto-detect JDK on macOS or standard PATH
if [ -x "/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/javac" ]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
elif /usr/libexec/java_home &>/dev/null; then
    export JAVA_HOME="$(/usr/libexec/java_home)"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "🔨 Compiling LibraFlow Pro (Java $(java -version 2>&1 | head -n 1))..."
mkdir -p bin
javac -d bin $(find src -name "*.java")

echo "🚀 Launching LibraFlow Pro..."
java -cp bin com.libraflow.Main "$@"
