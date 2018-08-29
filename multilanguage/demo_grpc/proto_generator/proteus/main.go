package main

import (
	"errors"
	"flag"
	"fmt"
	"os"
	"proteus/common"
)

type packageFlags []string

func (i *packageFlags) String() string {
	return fmt.Sprintf("%s", *i)
}

func (i *packageFlags) Set(value string) error {
	*i = append(*i, value)
	return nil
}

var packageInfo packageFlags

func main() {
	flag.Var(&packageInfo, "p", "To Accept the package path")
	var folderInfo = flag.String("f", "", "To Accept the folder path")
	flag.Parse()
	if *folderInfo == "" {
		fmt.Println("destination path cannot be empty")
		return
	}
	if len(packageInfo) <= 0 {
		fmt.Println(" Package path cannot be empty")
		return
	}
	packages := make([]string, 0)
	for i := 0; i < len(packageInfo); i++ {
		packages = append(packages, packageInfo[i])
	}
	var options common.Options
	options.BasePath = *folderInfo
	options.Packages = packages
	err := genProtos(options)
	if err != nil {
		fmt.Println(err)
	}
}
func genProtos(options common.Options) error {

	if err := checkFolder(options.BasePath); err != nil {
		return err
	}

	return common.GenerateProtos(options)
}

func checkFolder(p string) error {
	fi, err := os.Stat(p)
	switch {
	case os.IsNotExist(err):
		return errors.New("folder does not exist, please create it first")
	case err != nil:
		return err
	case !fi.IsDir():
		return fmt.Errorf("folder is not directory: %s", p)
	}
	return nil
}
