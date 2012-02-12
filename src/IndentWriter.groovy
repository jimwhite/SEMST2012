class IndentWriter extends PrintWriter
{
    protected boolean needIndent = true;
    protected String indentString;
    protected int indentLevel = 0;

    public IndentWriter(Writer w) { this(w, "  ", 0, true); }
    public IndentWriter(Writer w, String indent, int level, boolean needs)
    { super(w, true); indentString = indent; indentLevel = level; needIndent = needs }

    public int getIndent() { return indentLevel; }

    public IndentWriter plus(int i) {
        return new IndentWriter(out, indentString, indentLevel + i, needIndent);
    }

    public IndentWriter minus(int i) {
        return (plus(-i));
    }

    public IndentWriter next() { return plus(1); }
    public IndentWriter previous() { return minus(1); }

    protected void printIndent() {
        needIndent = false;
        super.print(indentString * indentLevel);
    }

    protected void checkIndent() { if (needIndent) { printIndent(); }; }

    public void println() { super.println(); needIndent = true; }

    public void print(boolean b) { checkIndent(); super.print(b); }
    public void print(char c) { checkIndent(); super.print(c); }
    public void print(char[] s) { checkIndent(); super.print(s); }
    public void print(double d) { checkIndent(); super.print(d); }
    public void print(float f) { checkIndent(); super.print(f); }
    public void print(int i) { checkIndent(); super.print(i); }
    public void print(long l) { checkIndent(); super.print(l); }
    public void print(Object obj) { checkIndent(); super.print(obj); }
    public void print(String s) { checkIndent(); super.print(s); }

// public void close() { }
// public void closeForReal() { super.close() }
}
