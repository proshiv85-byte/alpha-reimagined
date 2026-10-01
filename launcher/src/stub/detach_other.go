//go:build !windows

package main

import "syscall"

// detachedAttr puts the game in its own session so closing the terminal
// doesn't take it down.
func detachedAttr() *syscall.SysProcAttr {
	return &syscall.SysProcAttr{Setsid: true}
}

func attachParentConsole() {}
