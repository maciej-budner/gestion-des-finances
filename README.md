# Gestion des finances

Une application Android qui permet de vérifier combien il nous reste à la fin du mois et combien nous devons mettre de côté chaque mois.

# Modalités de téléchargement

Vous trouverez un fichier nommé APK Android, permettant de télécharger et d’installer l’application sur votre appareil Android.
J’ai également étudié la possibilité de rendre l’application disponible sur iPhone. Pour cela, il faudrait soit utiliser Flutter, ce qui impliquerait des frais de publication sur l’App Store, soit opter pour une PWA (Progressive Web App). Cette deuxième option est actuellement la meilleure.
Le site est accessible via [ce lien](https://maciej-budner.github.io/gestion-des-finances/). 
Si vous souhaitez l'installer comme une application, voici les étapes à suivre :
## 1. Sur iPhone (iOS)

Apple ne permet pas l'installation via un bouton dans le site, il faut passer par le menu de partage de Safari.
- Ouvre Safari et accède à l'URL de ton tableau de bord (ex: https://ton-site.com/iphone/dashboard.html).
- Appuie sur l'icône Partager (le carré avec une flèche vers le haut en bas de l'écran).
- Fais défiler les options vers le bas et appuie sur Sur l'écran d'accueil.
- Donne un nom à ton application (ex: "Ma Finance") et appuie sur Ajouter.
- L'icône apparaît maintenant sur ton écran d'accueil. En cliquant dessus, l'application s'ouvrira en plein écran (sans la barre d'adresse du navigateur).

## 2. Sur Android
C'est souvent plus automatique sur Android via Chrome.
- Ouvre Chrome et va sur ton site.
- Une petite bannière "Ajouter à l'écran d'accueil" apparaît souvent en bas. Sinon :
- Appuie sur les trois petits points en haut à droite.
- Sélectionne Installer l'application ou Ajouter à l'écran d'accueil.
- Valide, et l'icône sera ajoutée à ton menu d'applications.

## 3. Sur PC (Windows / Mac)
Si tu utilises Chrome ou Edge, tu peux transformer l'onglet en fenêtre indépendante.
- Ouvre ton site dans Chrome.
- Dans la barre d'adresse, tout à droite, tu verras une petite icône qui ressemble à un ordinateur avec une flèche (Installer).
- Si tu ne la vois pas, clique sur les trois points > Enregistrer et partager > Installer la page en tant qu'application.
- L'application s'ouvre alors dans une fenêtre séparée sans les barres d'outils du navigateur, et tu peux l'épingler à ta barre des tâches.

# Visuel de l’application et son fonctionnement sur l'appli Android (apk Android)

Dans cette application, vous pouvez suivre votre budget grâce à un graphique en forme de donut qui affiche vos dépenses et vos gains.

Vous disposez également d’une gestion de l’épargne, qui vous indique combien vous devez mettre de côté pour le mois en cours. Cette valeur se met automatiquement à jour en fonction des gains et des dépenses que vous ajoutez.

Il y a aussi une section correspondant à ce qu’il vous reste, considérée comme un budget plaisir.

<img src="Screenshot_20260121_205841_Management.jpg" width="200" height="400"/>

Nous pouvons également parcourir un tableau listant chaque dépense et chaque gain. Il est possible de les supprimer, par exemple si une donnée a été ajoutée par erreur ou si le salaire a changé.

Sous ce graphique, vous trouverez deux boutons :

le premier permet d’ajouter un gain ou une dépense (ponctuelle ou récurrente),

le second permet de mettre à jour le pourcentage d’épargne souhaité.

Chaque bouton ouvre une fenêtre contenant un formulaire à remplir.

Voici à quoi ressemble le bouton pour ajouter un gain ou une dépense :

<img src="Screenshot_20260121_205848_Management.jpg" width="200" height="400"/> <img src="Screenshot_20260121_205852_Management.jpg" width="200" height="400"/>

S’il y a une erreur dans le formulaire, celui-ci ne pourra pas être validé et les erreurs seront indiquées à l’écran.

<img src="Screenshot_20260121_205858_Management.jpg" width="200" height="400"/>

Le fonctionnement est similaire pour l’épargne, mais avec une seule donnée à renseigner :

<img src="Screenshot_20260121_205844_Management.jpg" width="200" height="400"/>
Informations importantes

Toute modification ou lancement de l’application prend environ 3 secondes. Cela est dû au fait que le fichier Excel utilisé contient des calculs limités à 1000 cellules.
Si ce nombre est dépassé, les nouvelles données ne seront plus calculées.

Afin d’éviter l’ouverture du fichier Excel alors qu’il n’est pas encore fermé, un temps d’attente (sleep) de 1 seconde a été ajouté.

# Visuel de l’application et son fonctionnement sur le site Web (PWA)

Dans cette page web, vous pouvez suivre votre budget grâce à un graphique en forme de donut qui affiche vos dépenses et vos gains.
vous serai diriger sur la page de connection où en peut s'inscrire.

<img src="connection.png" width="200" height="400"/>

Vous disposez également d’une gestion de l’épargne, qui vous indique combien vous devez mettre de côté pour le mois en cours. Cette valeur se met automatiquement à jour en fonction des gains et des dépenses que vous ajoutez.

Il y a aussi une section correspondant à ce qu’il vous reste, considérée comme un budget plaisir.

<img src="pageDash.png" width="200" height="400"/>

Nous pouvons également parcourir un tableau listant chaque dépense et chaque gain. Il est possible de les supprimer, par exemple si une donnée a été ajoutée par erreur ou si le salaire a changé.

Sous ce graphique, vous trouverez deux boutons :

le premier permet d’ajouter un gain ou une dépense (ponctuelle ou récurrente),

le second permet de mettre à jour le pourcentage d’épargne souhaité.

Chaque bouton ouvre une fenêtre contenant un formulaire à remplir.

Voici à quoi ressemble le bouton pour ajouter un gain ou une dépense :

<img src="ajoue1.png" width="200" height="400"/> <img src="ajoue2.png" width="200" height="400"/>

S’il y a une erreur dans le formulaire, celui-ci ne pourra pas être validé et les erreurs seront indiquées à l’écran.

<img src="erreur.png" width="200" height="400"/>

Le fonctionnement est similaire pour l’épargne, mais avec une seule donnée à renseigner :

<img src="ajoueEparne.png" width="200" height="400"/>

# Futures améliorations

- Dans le fichier Excel, une colonne permet déjà de calculer la quantité épargnée. Cela permettra plus tard d’afficher le montant total qui aurait dû être mis de côté depuis l’ajout du pourcentage d’épargne.

- Supprimer l’attente de 3 secondes au démarrage et mettre en place un affichage dynamique.

- Migrer l’application vers Flutter afin de proposer une version iPhone de mon application android.
