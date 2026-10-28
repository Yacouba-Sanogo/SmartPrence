# Lance le remplissage du compte de Moussa sur Render.
# Les identifiants administrateur sont demandes ici, dans votre terminal : ils restent chez vous.
$env:ADMIN_EMAIL = Read-Host "Email de l'administrateur Render"
$motDePasse = Read-Host "Mot de passe de l'administrateur" -AsSecureString
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $motDePasse).Password
node (Join-Path $PSScriptRoot "remplir-compte-etudiant.js")
Remove-Item Env:ADMIN_PASSWORD
Remove-Item Env:ADMIN_EMAIL
