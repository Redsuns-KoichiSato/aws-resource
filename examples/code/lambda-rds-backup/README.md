# RDS-BACKUP

Il progetto qui presente necessità della creazione a priori di due funzioni lambda senza implementazione di codice,
chiamate a vostro piacimento.
Una volta fatto ciò questa repository vi creerà due zip da caricare poi nelle vostre lamba.
Queste due zip contengono una funzione padre ed una funzione figlia. La funzione padre crea uno snapshot e chiama
la funzione figlia passandole come parametri gli attributi dello snapshot appena creato. La funzione figlia,
una volta ottenuti i dati dello snapshot appena creato dal padre, si occuperà di fare un export su un bucket S3,
specificato tramite variabili d'ambiente.

## DEPENDENCES
- Go v1.x
- Zip

### INSTALL GO
See this [link](https://go.dev/doc/install)

### INSTALL ZIP
See this [link](https://www.tecmint.com/install-zip-and-unzip-in-linux)

## GENERATE ZIP
Dare i permessi di esecuzione allo script:
``sudo chmod +X createZip.sh``

Eseguire lo script:
``./createZip.sh``

Questo script crea nella directory corrente due archivi che contengono le due funzioni, pronte per essere caricate
su AWS.

## DOCS
Guardare nella cartella `docs` per le configurazioni di policy e variabili d'ambiente.

###**OSS - È necessario, oltre alla creazione delle due funzioni:**
1. Creare una chiave "simmetrica" su 
[KMS](https://eu-west-3.console.aws.amazon.com/kms/home?region=eu-west-3#/kms/home) da utilizzare nelle variabili d'ambiente delle funzioni
2. Assegnare al ruolo i permessi come nell'immagine `docs/lambda_role_policy.png` con l'aiuto dei JSON
`docs/diegoPassRole.json` e `docs/invoke_async_lambda_diego_policy.json`
3. Assegnare al ruolo le entità attendibili come nell'immagine `docs/policy_entità_attendibili.png` con l'aiuto del JSON
`docs/policy_entità_attendibili.json`
4. Definire le variabili d'ambiente nelle due funzioni lambda create in 
**"lambdaFunction -> configurazione -> variabili d'ambiente"** come nell'immagine 
`docs/childFunction_env.png` e `docs/parentFunction_env.png`
