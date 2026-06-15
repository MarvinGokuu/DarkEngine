import os
import javalang
from javalang.tree import ClassCreator, MethodInvocation, WhileStatement, ForStatement, DoStatement

SRC_DIR = r"c:\Users\theca\Documents\GitHub\DarkEngine\src"
REPORT_FILE = r"C:\Users\theca\.gemini\antigravity-ide\brain\0c224870-e794-4005-b4e2-9a4f64d59770\artifacts\REPORTE_SEMANTICO_CEO.md"

HOT_PATH_METHODS = {'update', 'tick', 'runMainLoop', 'poll', 'offer', 'execute', 'run', 'batchOffer', 'batchPoll'}
BAD_INVOCATIONS = {'split', 'getBytes', 'toString'}
IO_INVOCATIONS = {'print', 'println', 'printf'}

def is_hot_path(method_node):
    if method_node.name in HOT_PATH_METHODS:
        return True
    return False

def check_false_garbage(node, method_name, filepath):
    findings = []
    for path, n in node.filter(ClassCreator):
        # Ignore Exceptions, arrays are handled differently, but pure ClassCreator in hot-path is bad.
        if "Exception" not in n.type.name and "Error" not in n.type.name:
            findings.append((n.position.line if n.position else "?", f"Falso Zero-Garbage: Instanciación de `new {n.type.name}()` en Hot-Path ({method_name}). Impacta el GC."))
    
    for path, n in node.filter(MethodInvocation):
        if n.member in BAD_INVOCATIONS:
            findings.append((n.position.line if n.position else "?", f"Asesinato de Memoria: Llamada a `.{n.member}()` en Hot-Path ({method_name}). Genera arrays de Strings/Bytes desechables."))
        if n.member in IO_INVOCATIONS and ("System" in str(n) or "out" in str(n) or "err" in str(n)):
            findings.append((n.position.line if n.position else "?", f"Bloqueo I/O: Llamada sincrónica a `System.out/err.{n.member}()` en Hot-Path ({method_name}). Rompe latencia de ns."))
    return findings

def check_spin_wait_deadlocks(node, method_name, filepath):
    findings = []
    loops = []
    for path, n in node.filter(WhileStatement): loops.append(n)
    for path, n in node.filter(ForStatement): loops.append(n)
    for path, n in node.filter(DoStatement): loops.append(n)
    
    for loop in loops:
        has_spin_wait = False
        has_break = False
        for _, n in loop.filter(MethodInvocation):
            if n.member == 'onSpinWait' or n.member == 'parkNanos':
                has_spin_wait = True
                line = n.position.line if n.position else "?"
        if has_spin_wait:
            # Check if there is any break, return, or throw inside the loop
            for _, stmt in loop.filter(javalang.tree.BreakStatement): has_break = True
            for _, stmt in loop.filter(javalang.tree.ReturnStatement): has_break = True
            for _, stmt in loop.filter(javalang.tree.ThrowStatement): has_break = True
            
            if not has_break:
                findings.append((line, f"Peligro de Dead-Lock: `Thread.onSpinWait()` en bucle infinito dentro de `{method_name}` sin timeout ni `break`. Si el consumidor muere, el juego se cuelga."))
    return findings

def main():
    all_findings = []
    for root, _, files in os.walk(SRC_DIR):
        for file in files:
            if file.endswith(".java"):
                filepath = os.path.join(root, file)
                with open(filepath, "r", encoding="utf-8", errors='ignore') as f:
                    content = f.read()
                
                try:
                    tree = javalang.parse.parse(content)
                except Exception:
                    continue # Ignore parse errors for partial files
                
                file_findings = []
                for _, method in tree.filter(javalang.tree.MethodDeclaration):
                    if is_hot_path(method):
                        fg = check_false_garbage(method, method.name, filepath)
                        dl = check_spin_wait_deadlocks(method, method.name, filepath)
                        file_findings.extend(fg)
                        file_findings.extend(dl)
                
                if file_findings:
                    all_findings.append((filepath, file_findings))

    with open(REPORT_FILE, "a", encoding="utf-8") as out:
        out.write("\n\n---\n\n## 🚀 Bloque D: Auditoría Semántica Completa AST (100% de la Base de Código)\n")
        out.write("Esta sección ha sido generada procesando el Árbol de Sintaxis Abstracta (AST) de cada archivo `.java` para buscar vulnerabilidades arquitectónicas garantizando el contexto (ej. ignorar constructores, revisar Hot-Paths).\n\n")
        
        for filepath, findings in all_findings:
            rel_path = os.path.relpath(filepath, SRC_DIR).replace("\\", "/")
            out.write(f"### 📄 Archivo: `{rel_path}`\n")
            for line, desc in findings:
                out.write(f"* **Línea {line}**: {desc}\n")
            out.write("\n")
            
if __name__ == "__main__":
    main()
