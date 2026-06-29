$env:JAVA_HOME = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory | Where-Object { $_.Name -like "jdk-21*" } | Select-Object -First 1).FullName
$env:PATH = "$env:JAVA_HOME\bin;D:\Proyectos\tools\apache-maven-3.9.16\bin;$env:PATH"
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NUL"
$env:JWT_SECRET = "ZWNvbm92YS1zZWNyZXQta2V5LWZvci1kZXYtb25seS0yMDI2LW1pZGpvdXJuZXk="

Set-Location "$PSScriptRoot\midjourney-backend"
mvn spring-boot:run
