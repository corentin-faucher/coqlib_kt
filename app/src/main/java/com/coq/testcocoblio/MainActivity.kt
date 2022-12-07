package com.coq.testcocoblio

import com.coq.coqlib.*
import com.coq.coqlib.graph.Icon
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.graph.scheduleGL
import com.coq.coqlib.nodes.*
import com.coq.coqlib.R as RC
import java.util.*


class MainActivity : CoqActivity(R.style.Theme_TestCocoblio, null, null) {
    override fun getExtraTextureTilings(): Map<Int, Texture.Tiling>? {
        // (pass)
        // Si besoin retourner la map des tiling des pngs du projets,
        // voir Texture.kt -> tilingOfDrawableRes comme exemple.
        return null
    }

    override fun getExtraSoundIds(): IntArray? {
        // (pass)
        // Si besoin ajouter les Ids de son supplémentaires (dans res/raw).
        return null
    }

    override fun getAppRoot(): AppRootBase {
        printdebug("Création l'horloge  de la structure (AppRoot)")
        return AppRoot(this)
    }
}

/** Noeud racine de la structure à afficher... */
class AppRoot(coqActivity: CoqActivity) : AppRootBase(coqActivity) {
    init {
        /** Y mettre : un écran de fond et un écran de "devant": */
        BackScreen(this)
        FrontScreen(this)
        /** Commencer dans un écran "FirstScreen" (voir plus bas pour FirstScreen...) */
        changeActiveScreenToNewOfClass(FirstScreen::class.java)
    }
    override fun willDrawFrame() {
        // (pass, rien à mettre à jour...)
    }
}

/** Un écran de fond. Ici un simple pavage de textures. */
class BackScreen(root: AppRoot) : Screen(root) {
    init {
        addFlags(Flag1.dontAlignScreenElements or Flag1.persistentScreen)
        var id1 = RC.drawable.the_cat
        var id2 = RC.drawable.some_animals
        for(i in 0 until 4) for(j in 0 until 8) {
            TiledSurface(this, id1,
                -3.5f + 2f*i.toFloat(), -3.5f + j.toFloat(),
                0.75f, 0f, j + i*4)
            TiledSurface(this, id2,
                -2.5f + 2f*i.toFloat(), -3.5f + j.toFloat(),
                0.6f, 0f, j + i*4)
            // Un "kotlin swap"... Pas très intuitif... mais ça marche.
            id1 = id2.also { id2 = id1 }
        }
        openAndShowBranch()
    }
}

/** Un écran "en avant" pour les effets de particules, pop over, curseur, ...
 * (vide à priori...) */
class FrontScreen(root: AppRoot) : Screen(root) {
    init {
        addFlags(Flag1.dontAlignScreenElements or Flag1.persistentScreen or Flag1.show)
        /** Init des statics pour les Popover...
         * (les popover s'affiche dans le "front" screen par défaut. */
        PopMessage.initWith(this, Texture.getPng(RC.drawable.frame_mocha))
        Sparkles.initWith(this)
    }
}

/** Premier écran de test */
class FirstScreen(root: AppRoot) : Screen(root) {
    init {
        // Un "bloc" avec quelques noeuds quelconques...
        Node(this, 0f, 0f, 1.0f, 1f, 0f,
            Flag1.reshapeableRoot
        ).also { block ->
            block.addFrame(RC.drawable.frame_white_back, 0.05f)
            ChangeLanguageButton(block, root, 0f, 0.25f, 1f,0.15f)
            FramedString(block, RC.drawable.frame_gray_back, R.string.english_only,
                0f, 0.4f, 0.5f,0.10f, flags = Flag1.reshapeableRoot)

            TestButton(block, root, -0.35f, -0.15f, 0.2f) {
                changeActiveScreenToNewOfClass(SecondScreen::class.java)
            }
            NumberNode(block, 152, 0.45f, 0.45f, 0.10f)
            val fontToTest = RC.font.yusei_magic
            TestString(block, "الخير 말하는", fontToTest,
                0.0f, 0.05f, 0.12f)
            TestString(block, "かな？说话的", fontToTest,
                0.15f, -0.20f, 0.10f)
            TestString(block, "jxéà Một sủa", fontToTest,
                0.00f, -0.40f, 0.12f)
        }
        Node(this, 0f, 0f, 1.2f, 2f, 0f
        ).run {
            scaleX.set(0.5f)
            scaleY.set(0.5f)
            addFrame(RC.drawable.frame_white_back, 0.05f)
            SlidingMenu(this, 4,
                0f, 0f, 1.2f, 2f, 1f, 0L
            ).run {
                // Largeur maximal pour le sliding menu.
                val width = itemRelativeWidth
                addItem(SliderTest(null, 0f, 0f, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Allo", 0f, 0f, width, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Bonjour", 0f, 0f, width, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Canard", 0f, 0f, 2.5f, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Chien qui jappe très fort !", 0f, 0f, width, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Chat", 0f, 0f, width, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Autruche", 0f, 0f, width, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Pingouin", 0f, 0f, width, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Capibara", 0f, 0f, width, 1f))
                addItem(FramedString(null, com.coq.coqlib.R.drawable.frame_mocha,
                    "Cochon d'inde", 0f, 0f, width, 1f))

            }
        }
        // Faire apparaître un Message après 1 seconde.
        // Ici, on utilise l'extension custom "scheduleGL"
        // qui exécute la tâche du timer dans la thread OpenGL... (Broche à foin?)
        Timer().scheduleGL(1000L) {
            PopMessage.over(this, "Bonjour !")
        }
    }
}

/** Deuxième écran de test */
class SecondScreen(root: AppRoot) : Screen(root) {
    init {
        // Exemple de bouton défini "on the fly".
        Node(this, 0f, 0f, 4f, 4f
        ).run {
            addFrame(RC.drawable.frame_gray_back, 0.1f)
            object : SwitchButton(
                this@run, false,
                0f, 1f, 1f
            ) {
                override fun action() {
                    // Un "popover", noeud qui se retire automatiquement de la structure.
                    PopMessage.over(
                        this, "Changé pour ${if (isOn) "On" else "Off"}",
                        null, 2f, isOn, 0.5f
                    )
                }
            }
            TiledSurface(this, Texture.getPng(RC.drawable.some_animals),
                0f, -0.5f, 0.85f, 0f)
        }

        Node(this, 0f, 0f, 1f, 1f
        ).run {
            scaleX.set(2f)
            scaleY.set(1.5f)
            TestButton(this, root, 0f, 0f, 0.4f) { changeActiveScreen(FirstScreen(root)) }
        }
    }
}

class SliderTest : SliderButton
{
    constructor(ref: Node?, x: Float, y: Float, height: Float
    ) : super(ref, 0f, true, x, y, height, 4f*height)
    private constructor(other: SliderTest) : super(other)
    override fun clone() = SliderTest(this)

    override fun action() {
        Sparkles.over(this)
    }
}

// Normalement on a pas besoin de reshaper les boutons...
// Ici on propage reshape juste pour mettre à jour les strings après changement de langue.
class ChangeLanguageButton(
    parent: Node, private val root: AppRoot,
    x: Float, y: Float, width: Float, height: Float
) : Button(parent, x, y, height, 0f, Flag1.reshapeableRoot)
{
    init {
        this.width.set(width, fix = true, setAsDef = true)
        addFrameAndString(RC.drawable.frame_mocha, R.string.hello)
    }
    override fun action() {
        val newLanguage: Language = if(Language.currentIs(Language.French)) Language.English
            else Language.French
        Language.current = newLanguage
        root.reshapeBranch()
    }
}

class TestButton(ref: Node, private val root: AppRoot, x: Float, y: Float, height: Float,
                 private val doAction: AppRoot.() -> Unit
) : SecureButtonWithPopover(ref, Icon.Help.ordinal, R.string.button,
    x, y, height, 0f, 0L)
{
    override fun action() {
        super.action()
        root.doAction()
    }
}





