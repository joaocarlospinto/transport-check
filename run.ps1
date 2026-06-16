$env:JAVA_HOME   = "C:\Program Files\Java\jdk-21.0.11"
$KEYTOOL         = "$env:JAVA_HOME\bin\keytool.exe"
$TRUSTSTORE      = "$PSScriptRoot\metro-truststore.jks"
$TRUSTSTORE_PASS = "metrostore"
$CERT_FILE       = "$PSScriptRoot\metro-api.cer"
$ALIAS           = "metrolisboa-api"

# Cria o truststore do projeto na primeira execucao
if (-not (Test-Path $TRUSTSTORE)) {
    Write-Host "Criando truststore do projeto (primeira execucao)..."

    # Descarrega o certificado do servidor da API
    $tcpClient = New-Object System.Net.Sockets.TcpClient("api.metrolisboa.pt", 8243)
    $sslStream  = New-Object System.Net.Security.SslStream(
        $tcpClient.GetStream(), $false,
        { param($s, $c, $ch, $e) $true }   # aceita para download apenas
    )
    $sslStream.AuthenticateAsClient("api.metrolisboa.pt")
    $certBytes = $sslStream.RemoteCertificate.Export(
        [System.Security.Cryptography.X509Certificates.X509ContentType]::Cert
    )
    $sslStream.Close(); $tcpClient.Close()
    [System.IO.File]::WriteAllBytes($CERT_FILE, $certBytes)
    Write-Host "Certificado descarregado para $CERT_FILE"

    # Importa para o truststore local (sem permissoes de administrador)
    & $KEYTOOL -importcert -trustcacerts -noprompt `
        -keystore $TRUSTSTORE `
        -storepass $TRUSTSTORE_PASS `
        -alias $ALIAS `
        -file $CERT_FILE
    Write-Host "Truststore criado em $TRUSTSTORE"
} else {
    Write-Host "Truststore do projeto ja existe."
}

# Carrega variaveis do .env
if (Test-Path "$PSScriptRoot\.env") {
    Get-Content "$PSScriptRoot\.env" | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
        }
    }
}

& "$env:JAVA_HOME\bin\java.exe" `
    "-Dmaven.multiModuleProjectDirectory=$PSScriptRoot" `
    -cp "$PSScriptRoot\.mvn\wrapper\maven-wrapper.jar" `
    org.apache.maven.wrapper.MavenWrapperMain `
    spring-boot:run
