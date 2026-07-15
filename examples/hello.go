package main

import "fmt"

func main() {
	name := "Go Portable"
	fmt.Printf("Hello from %s!\n", name)
	for i := 1; i <= 3; i++ {
		fmt.Println("count:", i)
	}
}
