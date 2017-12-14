# Usage:
# VERSION_1=1.2.1 VERSION_2=2.0.0-SNAPSHOT ./compare.sh
#
# Results:
# See the compat_reports folder

# Script dir
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# All FirebaseUI libraries
LIBRARIES=( firebase-ui-auth firebase-ui-database firebase-ui-storage firebase-ui-firestore )

for LIB in "${LIBRARIES[@]}"
do
    # Get AAR names
    VERSION_1_AAR="$LIB-$VERSION_1.aar"
    VERSION_2_AAR="$LIB-$VERSION_2.aar"

    cp ~/.m2/repository/com/firebaseui/$LIB/$VERSION_1/$VERSION_1_AAR .
    cp ~/.m2/repository/com/firebaseui/$LIB/$VERSION_2/$VERSION_2_AAR .

    # Unzip them into temp directories
    mkdir $VERSION_1 $VERSION_2

    unzip -d $VERSION_1 $VERSION_1_AAR
    unzip -d $VERSION_2 $VERSION_2_AAR

    # Compare them
    japi-compliance-checker --lib=$LIB \
        -skip-annotations-list $DIR/exclude-annotations.txt \
        $VERSION_1/classes.jar $VERSION_2/classes.jar

    # Remove AARs
    rm $VERSION_1_AAR $VERSION_2_AAR

    # Remove unzipped
    rm -r $VERSION_1 $VERSION_2
done
