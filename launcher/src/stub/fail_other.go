//go:build !windows

package main

import (
	"fmt"
	"os"
)

func fail(err error) {
	fmt.Fprintln(os.Stderr, "alphar:", err)
	os.Exit(1)
}
