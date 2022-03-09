#!/bin/bash

echo "Creazione binari..."
env GOOS=linux GOARCH=amd64 CGO_ENABLED=0 GO111MODULE=off go build -o ParentFunction/main ParentFunction/main.go
env GOOS=linux GOARCH=amd64 CGO_ENABLED=0 GO111MODULE=off go build -o ChildFunction/main ChildFunction/main.go
echo "Creazione zip..."
zip -f ParentFunction.zip ParentFunction/main
zip -f ChildFunction.zip ChildFunction/main
echo "Pulizia..."
rm ParentFunction/main
rm ChildFunction/main
