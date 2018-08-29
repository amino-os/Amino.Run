package report_custom

import (
	"fmt"
)

var silent bool
var testing bool
var msgStack []string

func MessageStack() []string {
	return msgStack
}

// Warn prints a formatted warn message to stdout.
func Warn(format string, args ...interface{}) {
	report("WARN", format, args...)
}

// Error prints a formatted error message to stdout.
func Error(format string, args ...interface{}) {
	report("ERROR", format, args...)
}

// Info prints a formatted info message to stdout.
func Info(format string, args ...interface{}) {
	report("INFO", format, args...)
}

func report(lvl string, format string, args ...interface{}) {
	fmt.Sprintf("%s: %s", fmt.Sprintf(format, args...))

	if testing {
		msgStack = append(msgStack, fmt.Sprintf("%s: %s", lvl, fmt.Sprintf(format, args...)))
	}

	if !silent || lvl == "ERROR" {
		fmt.Println(fmt.Sprintf("%s: %s", lvl, fmt.Sprintf(format, args...)))
	}
}
