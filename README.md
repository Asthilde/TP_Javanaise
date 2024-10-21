# TP_Javanaise
Ce projet est réalisé dans le cadre de l'UE Systèmes et Applications Réparties du Semestre 1 du M2 Génie Informatique de l'Université Grenoble Alpes.
Il est effectué par Salimata CISSOUMA et Noémie PELLEGRIN.

## Technologies utilisées
Ce projet se base sur le langage Java et l'API JavaRMI pour gérer l'envoi d'objets distants entre plusieurs machines virtuelles Java. Il est développé à l'aide de l'IDE Eclipse, ainsi les commandes pour exécuter le projet seront détaillées sous Eclipse.

## Fonctionnalités implémentées
Ce projet Javanaise implémente un système de création et gestion d'objets au sein de plusieurs JVM, dont l'accès en lecture et en écriture est controlé par un Coordinateur global, qui est l'équivalent d'un cache d'objet réparti.
L'objet initial, construit dans l'une des JVM, est transformé en objet distant au travers d'un objet d'interception, qui fait référence aux méthodes de l'objet applicatif. Actuellement, cet objet d'interception est encapsulé au sein d'un proxy, permettant aux JVM d'appeler directement les méthodes de l'objet référencé, sans connaître l'existante d'un objet d'interception et d'un objet applicatif distinct, ainsi que de la gestion des verrous.
Les fonctionnalités de verrouillage, déverrouillage du verrou sur l'objet d'interception, invalidation des lecteurs et écrivains en cours, terminaison correcte des JVM, sont implémentées.
Aucune fonctionnalités "bonus" n'a été implémentée, un tentative de résolution de l'interblocage a eu lieu mais n'est pas totalement fonctionnelle.

## Tests
Le test fourni initialement est fonctionnel est lancé avec un à 100 objets IRC.
Un test burst a été crée avec un classe Compteur et un lancement en boucle de plusieurs JVM essayant successivement de lire et écrire sur un objet compteur commun.
