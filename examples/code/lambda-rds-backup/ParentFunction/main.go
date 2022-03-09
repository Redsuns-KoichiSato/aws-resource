package main

import (
	"encoding/json"
	"fmt"
	"context"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/rds"
	lambdaService "github.com/aws/aws-sdk-go/service/lambda"
	"os"
	"time"
)

func main() {
	lambda.Start(HandleRequest)
}

func HandleRequest(ctx context.Context) (string, error) {
	// Creazione oggetto rds
	svc := rds.New(session.New())

	// Definizione parametri per snapshot RDS
	input := &rds.CreateDBSnapshotInput{
		DBInstanceIdentifier: aws.String(os.Getenv("DBInstanceID")),
		DBSnapshotIdentifier: aws.String(fmt.Sprintf("Snapshot-%v-Diego", time.Now().Unix())),
	}

	// Creazione snapshot
	result, err := svc.CreateDBSnapshot(input)
	if err != nil {
		fmt.Println("ERROR:", err)
		os.Exit(0)
	}

	// Invocazione funzione figlia
	invoke(result.DBSnapshot)

	return "DONE", nil
}


//invoke è una funzione che si occupa di chiamare la funzione figlia,
//passandole come parametri il json con le proprietà dello snapshot creato precedentemente.
func invoke(request *rds.DBSnapshot) {
	// Definizione sessione
	sess := session.Must(session.NewSessionWithOptions(session.Options {
		SharedConfigState: session.SharedConfigEnable,
	}))

	// Creazione client
	client := lambdaService.New(
		sess,
		&aws.Config{
			Region: aws.String(os.Getenv("Region")),
		},
	)

	// Creazione payload json
	payload, err := json.Marshal(request)
	if err != nil {
		fmt.Println(fmt.Sprintf(
			"ERROR_INVOKE: Error marshalling %s request\n%v",
			os.Getenv("InvokeFunctionName"),
			err,
		))
		os.Exit(0)
	}

	fmt.Println("DEBUG_JSON:", string(payload))

	// Chiamata della funzione figlia passandogli il JSON come parametro
	result, err := client.Invoke(&lambdaService.InvokeInput{
		FunctionName: aws.String(os.Getenv("InvokeFunctionName")),
		Payload:      payload,
	})

	// Controllo errore
	if err != nil {
		fmt.Println(fmt.Sprintf(
			"ERROR_INVOKE: Error %s\n%v",
			os.Getenv("InvokeFunctionName"),
			err,
		))
		os.Exit(0)
	}

	fmt.Println("DEBUG_INVOKE:", string(result.Payload))
}