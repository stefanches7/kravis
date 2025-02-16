package com.github.holgerbrandl.kravis

import java.io.File

/**
 * Adopted from vegas.render.StaticHTMLRenderer
 * @author HolgerAish Fenton.
 */
class StaticHTMLRenderer(val specJson: String) {

    fun importsHTML(additionalImports: List<String>): String {
        return (JSImports + additionalImports).map {
            "<script src=\"" + it + "\" charset=\"utf-8\"></script>"
        }.joinToString("\n")
    }

    fun headerHTML(vararg additionalImports: String) =
        """
       |<html>
       |  <head>
       |    ${importsHTML(additionalImports.toList())}
       |  </head>
       |  <body>
    """.trimMargin("|")

    fun plotHTML(name: String = this.defaultName): String {

        // see https://github.com/vega/vega-embed#vega-embed-opt-specification-reference
        return """
           | <div id='$name'></div>
           |
           | <script type="application/json" id="kravis_spec">
           | $specJson
           | </script>
           |
           | <script>
           |     var embedSpec = JSON.parse(document.getElementById('kravis_spec').innerHTML);
           |     var opt = {
           |      actions: false
           |     };
           |
           |     vegaEmbed('#$name', embedSpec, opt).then(function(result) {
           |        // access view as result.view
           |     }).catch(console.error);
           | </script>

        """.trimMargin("|")
    }

    val footerHTML =
        """
      |  </body>
      |</html>
    """.trimMargin("|")

    fun pageHTML(name: String = defaultName) = headerHTML().trim() + plotHTML(name) + footerHTML.trim()

    fun openInChrome(name: String = defaultName) {

        createTempFile("kravis", suffix = ".html").apply {
            writeText(pageHTML(defaultName))
        }.let {
            ProcessBuilder("open", it.absolutePath).start()
        }
    }

    /**
     * Typically you'll want to use this method to render your chart. Returns a full page of HTML wrapped in an iFrame
     * for embedding within existing HTML pages (such as Jupyter).
     * XXX Also contains an ugly hack to resize iFrame height to fit chart, if anyone knows a better way open to suggestions
     * @param name The name of the chart to use as an HTML id. Defaults to a UUID.
     * @return HTML containing iFrame for embedding
     */
    fun frameHTML(name: String = defaultName) {
        // https://stackoverflow.com/questions/1265282/recommended-method-for-escaping-html-in-java
        val frameName = "frame-" + name
        """
        <iframe id="${frameName}" sandbox="allow-scripts allow-same-origin" style="border: none; width: 100%" srcdoc="${escapeHTML(pageHTML(name))}"></iframe>
        <script>
          (function() {
            function resizeIFrame(el, k) {
              var height = el.contentWindow.document.body.scrollHeight || '400'; // Fallback in case of no scroll height
              el.style.height = height + 'px';
              if (k <= 10) { setTimeout(function() { resizeIFrame(el, k+1) }, 1000 + (k * 250)) };
            }
            resizeIFrame(document.querySelector('#${frameName}'), 1);
          })(); // IIFE
        </script>
    """.trimMargin()
    }

    fun escapeHTML(s: String): String {
        val out = StringBuilder(Math.max(16, s.length))
        for (i in 0 until s.length) {
            val c = s[i]
            if (c.toInt() > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#")
                out.append(c.toInt())
                out.append(';')
            } else {
                out.append(c)
            }
        }
        return out.toString()
    }


    private val WebJars = mapOf(
        "vega-lite" to "1.2.0",
        "vega" to "2.6.3",
        "d3" to "3.5.17"
    )

    //    private fun CDN(artifact: String, file: String) = "https://cdn.jsdelivr.net/webjars/org.webjars.bower/$artifact/${WebJars[artifact]}/$file"
    private fun CDN(artifact: String) = "https://cdnjs.cloudflare.com/ajax/libs/$artifact"

    val JSImports = listOf(
        "https://d3js.org/d3.v4.min.js",
        CDN("vega/3.0.7/vega.js"),
        CDN("vega-lite/2.0.1/vega-lite.js"),
        CDN("vega-embed/3.0.0-rc7/vega-embed.js")
    )

    val defaultName = "vegas-" + java.util.UUID.randomUUID().toString()

}

fun main(args: Array<String>) {
    val vlExample = """
  {
  "width": 600,
  "autosize": {
    "type": "pad",
  },
  "data": {
    "values": [
      {"a": "A","b": 28}, {"a": "B","b": 55}, {"a": "C","b": 43},
      {"a": "D","b": 91}, {"a": "E","b": 81}, {"a": "F","b": 53},
      {"a": "G","b": 19}, {"a": "H","b": 87}, {"a": "I","b": 52}
    ]
  },
  "mark": "bar",
  "encoding": {
    "x": {"field": "a", "type": "ordinal"},
    "y": {"field": "b", "type": "quantitative"}
  }
}
        """

    val json = File("src/test/resources/vl_regression/simple_scatter.json").readText()

    val renderer = StaticHTMLRenderer(json)
    //    val pageHTML = renderer.pageHTML()
    show(renderer.pageHTML())
    renderer.openInChrome()


    //    System.err.println(pageHTML)
    //
    //    show(pageHTML)
    //        SwingFXWebView().apply {
    //            showInPanel()
    //            Thread.sleep(3000)
    //            loadPage(pageHTML)
    //        }
}
