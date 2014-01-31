#!/bin/bash

##
## preprocessor.sh
## ---------------
## 
## Script pour passer le préprocesseur sur un fichier.
## Résoud le fichier en entrée par le préprocesseur,
## puis supprime les lignes ajouté par celui-ci.
## On obtient donc le fichier de base,
## avec les résolutions du préprocesseur,
## et on garde les directives includes intactes
## pour le recognizer.
##
## -- GOOL --
##
##
## Usage : ./preprocessor.sh pathInput pathTemporary pathOutput
##       : - pathInput -> le chemin du fichier en entrée.
##       : - pathTemporary -> le chemin d'un fichier qui sera créé temporairement pour le traitement.
##       : - pathOutput -> le chemin de destinnation du fichier en sortie.
##



## Les fichiers de base :

# Le fichier en entrée.
FILE_INPUT=$1

# Le fichier passer par le préprocesseur.
FILE_PREPRO=$2

# Le fichier de sortie.
FILE_OUT=$3



## Récupération des includes du fichier de base :

# La liste complète des includes avec leurs lignes.
REAL_LIST_INCLUDES=$(cat $FILE_INPUT | grep "\#include" -n)

# La liste complète des lignes où un include est présent.
REAL_LINE_INCLUDES=$(echo "$REAL_LIST_INCLUDES" | cut -d":" -f1)

# La liste complète des noms d'includes présents.
REAL_NAME_INCLUDES=$(echo "$REAL_LIST_INCLUDES" | cut -d" " -f2)



## Génération du fichier préprocessé :

g++ -E $FILE_INPUT > $FILE_PREPRO



## Récupération des lignes de breakpoint du fichier préprocessé :

# Liste des breakpoint du fichier préprocessé.
LIST_BREAKPOINT="$(cat $FILE_PREPRO | grep "\# "[[:digit:]] -n)"

# Lignes des breakpoint du fichier préprocessé.
LINES_BREAKPOINT=$(echo "$LIST_BREAKPOINT" | cut -d":" -f1)

# Noms des breakpoint du fichier préprocessé.
NAMES_BREAKPOINT=$(echo "$LIST_BREAKPOINT" | cut -d" " -f3)

# Nombre de ligne du fichier préprocessé.
LINE_FILE_PREPRO=$(cat $FILE_PREPRO | wc -l | cut -d" " -f1)



## Le main avec la création du fichier $FILE_OUT :

# Variable permettant de savoir s'il faut écrire.
to_print=0

# Indice de la ligne à traiter.
line_numer=1

# La ligne de début à écrire.
head=

# Indice des includes réel.
i_include=1

# Conteneur pour un include réel.
real_include=""

# Variable permettant de savoir s'il y a une erreur d'include réel.
fail_include=0

# On parcours tous les breakpoints pour réaliser le traitement.
for breakpoint in $NAMES_BREAKPOINT
do
    # On récupère la ligne à traiter.
    line=$(echo $LINES_BREAKPOINT | cut -d" " -f$line_numer)

    # On test s'il faut écrire jusqu'à line.
    if [[ $to_print -eq 1 ]]
    then
	# Cas à ignorer.
	if [[ $breakpoint = "\"<command-line>\"" ]]
	then
	    to_print=0
	else
	    # Cas on écrit.
	    # On décrémente pour enlever le breakpoint.
	    line=$((line-1))

	    # Cas particulier d'une seul ligne à écrire.
	    if [[ $line -lt $head ]]
	    then
		to_print=0
		real_include=$(echo $REAL_NAME_INCLUDES | cut -d" " -f$i_include)
		i_include=$((i_include+1))

		# Test si l'affichage d'un include réel est nécessaire.
		if [[ -n $real_include ]]
		then
		    # On écrit l'include réel.
		    echo "#include $real_include" >> $FILE_OUT
		else
		    # Sinon on garde l'erreur.
		    fail_include=1
		fi
	    else
		# Cas on écrit.
		# La ligne de début d'écriture à la ligne de fin.
		printFmt="$head,$line!d"
		# Ecriture de $head à $line.
		sed $printFmt $FILE_PREPRO >> $FILE_OUT

		to_print=0
		real_include=$(echo $REAL_NAME_INCLUDES | cut -d" " -f$i_include)
		i_include=$((i_include+1))

		# Test si l'affichage d'un include réel est nécessaire.
		if [[ -n $real_include ]]
		then
		    # On écrit l'include réel.
		    echo "#include $real_include" >> $FILE_OUT
		else
		    # Sinon on garde l'erreur.
		    fail_include=1
		fi
	    fi
	fi
    fi

    # Cas d'un breakpoint avec le nom du fichier source.
    if [[ $breakpoint = "\"$FILE_INPUT\"" ]]
    then
	# On supprime la ligne en conséquence.
	# Et on demande l'affichage.
	if [[ $fail_include -eq 1 ]]
	then
	    head=$(($line+2))
	else
	    head=$(($line+1))
	fi
	to_print=1
    fi

    # MAJ de la ligne de traitement.
    line_numer=$(( line_numer + 1 ))
done

# On traite le cas de fin du fichier préprocessé.
line=$LINE_FILE_PREPRO
if [[ $to_print -eq 1 ]]
then
    # La ligne de début d'écriture à la ligne de fin.
    printFmt="$head,$line!d"
    # Ecriture de $head à $line.
    sed $printFmt $FILE_PREPRO >> $FILE_OUT

    toPrint=0
fi



## Nettoyage :
# Suppression du fichier prepo.
rm $FILE_PREPRO



## EOF ##
