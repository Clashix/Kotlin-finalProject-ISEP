# 🎮 Application de Notation de Jeux Vidéo

Application mobile permettant aux joueurs de noter et commenter des jeux vidéo, et aux éditeurs de gérer leurs jeux avec des statistiques détaillées.

## 📱 Description

Cette application offre une plateforme complète pour la notation et l'évaluation de jeux vidéo. Elle permet aux joueurs de découvrir, noter et partager leurs avis sur leurs jeux préférés, tandis que les éditeurs peuvent gérer leur catalogue et suivre les performances de leurs jeux grâce à un tableau de bord analytique.

## 🧩 Fonctionnalités

### 👥 Pour les Joueurs

- **Authentification** : Inscription et connexion sécurisées via Firebase
- **Catalogue de jeux** : Consultation de la liste complète des jeux disponibles
- **Recherche avancée** : Recherche de jeux par titre, genre ou éditeur
- **Détails des jeux** : Accès aux informations complètes (description, note moyenne, avis)
- **Notation** : Système de notation de 1 à 5 étoiles avec commentaires
- **Historique personnel** : Consultation de tous vos avis et notes
- **Bibliothèque** : Possibilité de marquer les jeux comme "joués"
- **Intégration Steam** : Récupération de données depuis l'API Steam (si disponible)

### 🏢 Pour les Éditeurs

- **Authentification dédiée** : Inscription et connexion en tant qu'éditeur
- **Gestion de catalogue** : Ajout, modification et suppression de jeux
- **Tableau de bord** : Accès à des statistiques complètes :
  - Évolution des notes dans le temps
  - Distribution des notes (1 à 5 étoiles)
  - Nombre d'avis par jour
  - Moyenne quotidienne des notes
- **Visualisations** : Graphiques interactifs pour analyser les performances

### ⚙️ Fonctionnalités Techniques

- **Gestion de profils** : Photo de profil, nom, type de compte (joueur/éditeur)
- **Upload d'images** : Stockage des images de jeux et profils via Firebase Storage
- **Sécurité** : Règles Firestore pour sécuriser les accès aux données
- **Calcul automatique** : Statistiques calculées automatiquement via Cloud Functions
- **Internationalisation** : Gestion multilingue avec i18n
- **Sauvegarde** : Backup automatique avec Firebase
- **API Steam** : Intégration pour récupérer la liste des jeux

## 🛠️ Technologies

- **Langage** : Kotlin
- **Framework UI** : Jetpack Compose
- **Backend** : Firebase (Authentication, Firestore, Storage, Cloud Functions)
- **API** : Steam API
- **Architecture** : Clean Architecture
- **Gestion d'état** : State Management optimisé
- **Internationalisation** : i18n

## 📋 Prérequis

- Android Studio
- JDK 11 ou supérieur
- Compte Firebase configuré

## 🚀 Installation

1. Cloner le repository

```bash
git clone [url-du-repo]
```

2. Ouvrir le projet dans Android Studio

3. Configurer Firebase :

   - Ajouter le fichier `google-services.json` dans le dossier `app/`
   - Configurer les règles Firestore dans la console Firebase

4. Synchroniser Gradle et lancer l'application

Projet développé dans le cadre du cursus ISEP.
