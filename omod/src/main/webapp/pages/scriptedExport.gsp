<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeCss("reportingui", "codemirror-4.8/codemirror.css")
    ui.includeJavascript("reportingui", "codemirror-4.8/codemirror.js")
    ui.includeJavascript("reportingui", "codemirror-4.8/mode/sql/sql.js")
    ui.includeJavascript("reportingui", "codemirror-4.8/mode/groovy/groovy.js")
    ui.includeJavascript("reportingui", "codemirror-4.8/mode/velocity/velocity.js")
%>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.escapeJs(ui.message("reportingui.reportsapp.home.title")) }", link: emr.pageLink("reportingui", "reportsapp/home") },
        { label: "${ ui.escapeJs(ui.message("reportingui.scriptedExport.label")) }", link: "${ ui.escapeJs(ui.thisUrl()) }" }
    ];
</script>

<h1>${ ui.message("reportingui.scriptedExport.label") }</h1>

<style>
    .CodeMirror { height: 400px; border: 1px solid #ddd; font-size:smaller;}
</style>

<form method="post">

    <p>
        <label for="script-type-selector">${ ui.message("reportingui.scriptedExport.scriptType") }</label>
        <select id="script-type-selector" name="scriptType">
            <option value="sql">SQL</option>
            <option value="hql">HQL</option>
            <option value="groovy">Groovy</option>
        </select>
    </p>
    <p>
        <textarea id="editorTextArea" name="script"></textarea>
    </p>
    <p>
        <button>
            <i class="icon-save"></i>
            ${ ui.message("emr.save") }
        </button>

        <button>
            <i class="icon-play"></i>
            ${ ui.message("reportingui.adHocReport.runLink") }
        </button>
    </p>

</form>

<script type="text/javascript">

    var editor = CodeMirror.fromTextArea(document.getElementById('editorTextArea'), {
        lineNumbers: true,
        mode: "text/x-mysql",
        matchBrackets: true,
        indentUnit: 4,
        indentWithTabs: true
    });

    jq(function() {
       jq("#scriptTypeSelector").change(function(event) {
           var type = jq(this).val();
           if (type == 'sql' || type == 'hql') {
               type = 'text/x-mysql';
           }
           editor.setOption("mode", type);
       });
    });

</script>
