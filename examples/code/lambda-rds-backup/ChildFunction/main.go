package main

import (
	"context"
	"fmt"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/rds"
	"os"
)

func HandleRequest(ctx context.Context, event rds.DBSnapshot) (string, error) {
	// Stampo a video l'evento di input (ciò che ha inviato la funzione padre)
	fmt.Println("DEBUG_INPUT:", event)

	// Creazione oggetto rds
	svc := rds.New(session.New())

	// Definizione parametri di export
	params := &rds.StartExportTaskInput{
		ExportTaskIdentifier: aws.String("exportTask-" + *event.DBSnapshotIdentifier),
		IamRoleArn:           aws.String(os.Getenv("IamRoleArn")),
		KmsKeyId:             aws.String(os.Getenv("KmsKeyId")),
		S3BucketName:         aws.String(os.Getenv("S3BucketName")),
		SourceArn:            event.DBSnapshotArn,
	}

	// Invio richiesta di export
	result, err := svc.StartExportTask(params)
	if err != nil {
		fmt.Println("ERROR:", err)
		os.Exit(0)
	}

	fmt.Println("DEBUG_RESPONSE:", result)
	return fmt.Sprintf("DONE:", result), nil
}

func main() {
	lambda.Start(HandleRequest)
}
