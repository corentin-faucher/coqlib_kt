# coqlib kotlin

Librairie pour petit projet Android avec OpenGL.

## Installation

1. Installer Android Studio.
2. Cloner le répertoire :
```bash
  $ git clone git@github.com:corentin-faucher/coqlib_kt.git coqlib
```
3. Essayer l'"app" de test : Android Studio -> Open -> coqlib (répertoire cloné) ... (Trust project ; laisser Gradle synchroniser...)
4. Run "app".


## Nouveau projet utilisant coqlib

1. Créer un nouveau projet Android Studio : File -> New -> New Project.
2. Sélectionner : Empty Activity ; "My Application" ; language "kotlin" ; -> finish. Laisser Gradle synchroniser...
3. Ajouter coqlib au projet : Gradle Scripts (project) -> settings.gradle, à la fin du fichier modifier pour :
```
  include ':app', ':coqlib'
  // Mettre le chemin vers le module coqlib du projet coqlib.
  project(':coqlib').projectDir = new File("../coqlib/coqlib/")
```
4. Ajouter aussi comme dépendance du module "app". Gradle Scripts -> build.gradle du module project_name.app, ajouter aux dependencies:
```
  dependencies {
    implementation project(':coqlib')
    ...
  }
```
5. Synchroniser Gradle -> "Sync Now" au popup en haut à droite...
  Le module "coqlib" devrait maintentant apparaître dans "Project".
6. Toujours dans Gradle Scripts -> build.gradle du module project_name.app, vérifier aussi que les minSdk et targetSdk sont les même que le module coqlib :
```
  android {
    ...
    defaultConfig {
      ...
      minSdk 26
      targetSdk 34
      ...
    }
    ...
  }
```
7. (Vérifier si on a la bonne version de kotlin dans Gradle Scripts -> build.gradle (du projet)) :
```
plugins {
  ...
  id 'org.jetbrains.kotlin.android' version '1.8.20' apply false
}
```
8. Tester si l'app compile : Gradle -> "Sync Now", Run 'app'.


## Afficher une sprite OpenGL

Ajouter OpenGL dans le app->manifests->AndroidManifest.xml:
```html
  <manifest ...
  
  <uses-feature android:glEsVersion="0x00020000" android:required="true" />
  ...
```
Modifier le MainActivity pour un CoqActivity (effacer l'implémentation existante) :
  app -> java -> com...myapplication -> MainActivity.
```kotlin
  class MainActivity : CoqActivity(R.style.Theme_MyApplication, null, null, null)
  {
  }

```
### Implémenter les méthodes supplémentaires de CoqActivity.

1. getExtraTextureTilings : Méthode qui définie le nombre de tile m x n des pngs ajoutés dans res -> drawables. (On pass pour l'instant... Pas d'extra pngs.)
```kotlin
    override fun getExtraTextureTilings(): Map<Int, Texture.Tiling>? {
        // (pass)
        return null
    }
```
2. getExtraSoundIdsWithVolumeIds : Méthode qui définie les sons supplémentaires à charger par le SoundManager  (wav dans R.raw).
```kotlin
   override fun getExtraSoundIdsWithVolumeIds(): Array<Pair<Int, Int>>? {
        // pass...
        return null
    }
```
3. getAppRoot : Méthode qui permet d'obtenir la structure de base à afficher.
  Ici, on va juste créer une root "on the fly" avec une surface à afficher.
```kotlin
    override fun getAppRoot(): AppRootBase {
        return object : AppRootBase(this@MainActivity) {
            init {
                val the_cat = TiledSurface(this, drawable.the_cat,
                    0f, 0f, 1f,0f)
                the_cat.addFlags(Flag1.show)
            }
            // Écran d'acceuil (pour l'instant on n'affiche qu'une sprite)
            override val openingScreen: Class<out Screen>? = null
            override fun willDrawFrame() {
                // (pass, action sur la structure exécutée à chaque frame)
            }
            override fun didResume(sleepingTimeSec: Float) {
                // pass, action lorsque l'on sort de veille.
            }
            override fun willSleep() {
                // pass
            }
        }
    }

```
Ici, on a importé le png "the_cat.png" des drawable de coqlib en ajoutant l'import: 
`import com.coq.coqlib.R.drawable as drawable`.

Pour plus d'exemples, voir le module coqlib -> app.
