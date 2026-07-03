# Skyblock Togaether

Mod NeoForge pour Minecraft Java **1.21.1** (NeoForge **21.1.235**).

Ajoute une dimension skyblock reliée à son propre Nether et à son propre End. Chaque portail créé dans l'overworld génère une île flottante dans un monde entièrement vide, avec au centre un **bloc infini** qui fournit des ressources selon une progression configurable.

## Compilation

Le projet utilise Gradle + [ModDevGradle](https://github.com/neoforged/ModDevGradle). Prérequis : **JDK 21**.

**Option A — IntelliJ IDEA (recommandé)** : ouvrir le dossier du projet, IDEA télécharge Gradle 8.10.2 et les dépendances automatiquement. Le jar se construit avec la tâche Gradle `build` (résultat dans `build/libs/togaether-1.0.0.jar`).

**Option B — ligne de commande** : avec Gradle 8.8+ installé :

```
gradle wrapper        # génère le wrapper (une seule fois)
./gradlew build
```

Pour tester en dev : `./gradlew runClient` ou `./gradlew runServer`.

Le jar final se place dans le dossier `mods/` du serveur **et** des clients (le mod ajoute des blocs/items, il faut donc l'avoir des deux côtés).

## Fonctionnement

### Portail céleste (overworld → skyblock)
Construire un cadre en **pierre taillée** (chiseled stone bricks, configurable) comme un portail du Nether (intérieur de 2x3 minimum, jusqu'à 21x21), puis l'activer avec une **Clé céleste** (craft : 8 lingots d'or autour d'une perle d'Ender).

- Chaque portail génère une île flottante aux coordonnées correspondantes (échelle 1:1) dans la dimension skyblock, à condition d'être à plus de **250 blocs** (configurable) d'une île existante — sinon le portail est relié à l'île la plus proche, comme les portails du Nether en vanilla.
- Chaque île possède un **portail de retour** déjà allumé qui ramène exactement à l'endroit où le portail d'origine a été créé (et le reconstruit s'il a été détruit).

### L'île
Île d'herbe/terre/pierre avec un arbre, un coffre de départ (pousse, pain, seau d'eau...), le **bloc infini** au centre et le portail de retour.

### Bloc infini
Il ne se casse jamais. Il a de l'**XP** et un **niveau** (stockés sur le bloc, donc partagés par l'équipe de l'île), et fonctionne par **cycles** : chaque cycle est un pool d'objets (tirage pondéré), avec un niveau minimum pour être sélectionné et une quantité d'XP donnée au bloc à chaque minage.

**Clic droit** ouvre l'interface : XP, niveau et XP du prochain niveau en haut ; à gauche une navigation à deux niveaux — **la liste des catégories** (Nature, Minage, Savoir, Combat, Nether, End, Spécial — champ `category` de chaque cycle, libre dans la config), puis les cycles de la catégorie ouverte avec bouton « < Catégories » (l'actif en vert, les verrouillés grisés, molette/flèches) ; à droite le **détail du cycle consulté avec l'icône de chaque objet, sa quantité et son taux de drop en %**, et un bouton « Activer ce cycle » (grisé si le niveau du bloc est insuffisant). Le cycle actif détermine ce que le bloc drop. À l'ouverture, l'interface s'ouvre directement dans la catégorie du cycle actif.

Les drops sont tirés au sort selon les **poids** de la config (décimaux acceptés) : un objet à `"weight": 0.05` face à un total de 100 tombe ~1 fois sur 2000 — idéal pour du super-rare. Plusieurs cycles peuvent partager le même niveau requis.

Dans le panneau de détail, le nom et le taux de chaque objet sont **colorés selon leur rareté** : orange < 0,1 % (légendaire), mauve 0,1-1 % (épique), bleu 1-5 % (rare), vert 5-15 % (peu commun), blanc au-delà (commun).

Chaque cycle définit aussi une **dureté** (`hardness`, pierre = 1,5, obsidienne = 50 — les cycles par défaut vont de 0,6 pour les entraînements à **50 pour Légendaire**, soit ~9 s par bloc à la pioche en diamant, ~2 s en netherite Efficacité V) et un **outil de prédilection** (`tool` : `pickaxe`, `axe`, `shovel`, `hoe`, `sword` ou `any`) : miner le bloc avec le bon outil applique la vitesse réelle de l'outil (matériau, Efficacité, Célérité...), comme si on minait de la pierre avec une pioche ou une bûche avec une hache. Sans le bon outil, c'est lent. L'outil et la dureté du cycle consulté sont affichés dans le panneau de détail.

**Philosophie de rareté** — les cycles de récolte donnent ~80 % de matériaux de masse (pierre, bois, terre, netherrack... faciles à obtenir en volume). Les minerais sont nettement plus rares que l'exploration vanilla (fer brut ~3 %, lingot d'or ~2 %, diamant < 1 %). Et tout ce qui **débloque une source renouvelable** est extrêmement rare : pousses d'arbres (~0,05-0,1 %), graines et plantes cultivables (algues 0,06 %, canne à sucre, cactus...), œufs de créatures reproductibles (~0,01-0,04 %), selles, œufs de sniffer/tortue, verrues du Nether... Obtenir un de ces objets est un événement qui change la partie — ensuite la ferme prend le relais.

**Équilibrage** — les cycles ont trois rôles qui créent de vrais choix :

- **Récolte** (les 50 cycles thématiques) : le loot est le but, l'XP est faible (~0,6 × niveau par minage). Farmer des ressources ne fait pas monter vite.
- **Entraînement I à V** (niv. 1, 8, 16, 26, 36) : loot quasi nul (pavé, terre...) mais XP par minage très élevée et minage rapide (dureté 0,6, tous outils). C'est la voie rapide vers les niveaux — au prix de ne rien récolter d'utile.
- **Jackpot** (« Casino », niv. 22, et les cycles Trésors/Légendaire) : minage lent, ~aucune XP, mais uniquement des objets de valeur.

La courbe des seuils est **exponentielle** : départ accessible (niv. 2 à 16 XP, niv. 10 à ~900), milieu soutenu (niv. 20 ≈ 4 700, niv. 30 ≈ 25 000), fin de partie massive (niv. 40 ≈ 225 000, niv. 45 ≈ 746 000, **niv. 50 ≈ 2 530 000 XP**). En jouant optimalement avec les cycles d'entraînement, le niveau 50 demande ~54 000 minages — un objectif de très long terme pensé pour une équipe entière sur la durée d'un serveur ; en ne farmant que la récolte, bien plus encore. Le ramassage du bloc (casser accroupi) conserve XP, niveau et cycle dans l'objet.

La config par défaut embarquée compte **50 niveaux et 56 cycles** (50 thématiques + 5 entraînements + Casino, ~620 objets) suivant la progression du jeu : Bûcheron, Terrassier, Fermier... puis Nether, Netherite, End, Archéologie, Chambres d'essai, jusqu'au cycle Légendaire (œuf de dragon à 0,07 %, étoile du Nether, balise, heavy core...). Chaque cycle contient au moins un objet ultra-rare (< 0,1 %). Pour la régénérer après modification, supprime `config/togaether-cycles.json` et relance.

Cycles par défaut :

| Cycle | Niveau requis | XP/minage | Contenu (exemples) |
|---|---|---|---|
| Débuts | 1 | 1 | bois, terre, pierre, pousses, graines |
| Ferme | 2 | 1 | eau, lave, sable, argile, cultures |
| Mines | 3 | 2 | minerais (fer, cuivre, or...), œufs d'animaux |
| Nether | 4 | 3 | obsidienne, netherrack, bâtons de Blaze |
| Trésors | 5 | 4 | diamant, émeraude, débris antiques, perles |
| End | 6 | 5 | pierre de l'End, yeux d'Ender, **cadres de portail de l'End**, élytres |

Niveaux par défaut (XP cumulée) : 0, 30, 100, 300, 600, 1000. Tout est modifiable dans `config/togaether-cycles.json` (créé au premier lancement) : seuils de niveaux, cycles, objets, quantités, poids, XP.

**Objets admin** (créatif uniquement, onglet du mod) : 4 Éclats d'XP (+1, +10, +1000, +10000), consommés par clic droit sur un bloc infini pour lui rendre son XP — utile si un bloc a été détruit par accident (créatif + accroupi le détruit réellement ; un bloc reposé repart de zéro).

### Nether et End skyblock
- Un portail du Nether (obsidienne + briquet) construit dans le skyblock mène au **Nether skyblock**, vide, avec échelle 1:8 et liaison de portails comme en vanilla. Retour idem.
- Un portail de l'End (cadres obtenus via le bloc infini + yeux d'Ender) mène à l'**End skyblock**, vide, avec plateforme d'obsidienne et portail de retour vers l'île.
- Les trois dimensions sont étanches : impossible d'atteindre le vrai Nether/End depuis le skyblock, et inversement l'overworld reste intact.

### Configuration
`config/togaether-common.toml` : hauteur des îles, distance minimale entre îles, bloc du cadre, consommation de la clé, hauteur des portails du Nether.
`config/togaether-progression.json` : paliers du bloc infini.

## Structure du code

- `block/SkyPortalBlock` — portail custom (implémente `Portal` 1.21) : création/liaison d'îles et retour à l'ancre
- `portal/SkyPortalShape` — validation du cadre ; `portal/PortalBuilder` — construction de portails
- `world/IslandGenerator` — génération de l'île ; `world/IslandsData` — persistance (îles, ancres, retours de l'End)
- `event/PortalEvents` — redirection des portails Nether/End dans les dimensions skyblock + allumage des portails du Nether hors overworld
- `event/InfiniteBlockEvents` + `block/InfiniteBlock` — logique du bloc infini
- `config/` — config TOML (NeoForge) et progression JSON
- `data/togaether/dimension*` — les trois dimensions (générateur plat vide)

## Notes

- Testé pour NeoForge 21.1.235 ; toute version 21.1.x récente devrait convenir (`neo_version` dans `gradle.properties`).
- Le monde skyblock utilise un générateur plat sans couches : aucun bloc n'y est généré naturellement.
- Les portails du Nether côté skyblock utilisent les vrais blocs de portail vanilla, donc les mécaniques habituelles (POI, liaison au plus proche) s'appliquent.
