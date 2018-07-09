# Translations

All string resources in FirebaseUI are translated into a number of locales. The
translation process leverages Google's internal translation pipeline, so the
commands listed in this document are google specific.

The translation process has a few basic steps:

  1. Export strings from the git project
  1. Create a CL with the exported strings
  1. Kick off translation by requesting "extraction"
  1. Wait ~1 week for strings to be translated
  1. Finish translation by requesting a "dump"
  1. Export the dumped strings
  1. Import the dumped strings, and clean them up

## 1 - Export Strings

From the project root, run:

```shell
# This command will output to the console, you probably want to redirect
# it to some temporary file.
python ./scripts/translations/export_translations.py
```

This will take the `strings.xml` file from the `auth` module and get it
ready to be sent off for translation. Thist mostly consists of stripping
the `fui_` prefixes.

## 2 - Create a CL

Overwrite the following file with the output from the previous step:

```
google3/firebase/auth/ui/android/src/main/res/values/strings.xml
```

## 3 - Request Extraction

In the Translations Console (tc/) go to the **FirebaseUI** project and
click **Extract Source Messages**.

## 4 - Wait

Get a snack. Probably a few. You can check translation percentage in the
console.

## 5 - Request Dump

This is the same as step 3, but click **Dump Translations**. Make sure all
translations are 100% completed before doing this.

## 6 - Export Strings

In order to build out the `xml` files you'll need, run:

```shell
blaze build firebase/auth/ui/android:translated_messages
```

## 7 - Import and Cleanup Strings

First, copy the strings you just built back into the project:

```shell
cp -r blaze-genfiles/firebase/auth/ui/android/src/main/res/values* $FBUI/auth/src/main/res/
```

Second, in Android studio right-click on the folder containing all the
auth `values` XML files and click **Reformat Code**. I don't know of any
way to run an equivalent formatter at the command line.

Third, run the following from your project root:

```
./scripts/translations/clean_up_translations.sh
```
