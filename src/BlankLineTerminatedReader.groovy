
/**
 * A filter for BufferedReader that will split up sequences of lines separated by one or more blank lines.
 */
public class BlankLineTerminatedReader extends BufferedReader implements Iterator
{

    boolean inSection = false
    String currentLine = ''

    BlankLineTerminatedReader(Reader reader)
    {
        super(reader)
    }

    // Read the next line in this section.
    // Return null if we've reached the end of the section or the end of the file.
    public String readLine()
    {
        if (inSection) {
            // The currentLine will be null if our previous readLine hit EOF.
            if (currentLine != null) {
                String thisLine = currentLine.trim()

                if (!thisLine.isEmpty()) {
                    // This line isn't blank, so we prime for next time and let them have this one.
                    currentLine = super.readLine()
                    return thisLine
                }
            }

            inSection = false
        }

        return null
    }

    /**
     * We only close if the underlying reader is at EOF
     */
    public void close() 
    {
        if (!hasNext()) super.close()
    }

    @Override
    boolean hasNext()
    {
        if (currentLine != null) {
            while (currentLine.isEmpty()) {
                currentLine = super.readLine()

                if (currentLine == null) break

                currentLine = currentLine.trim()
            }
        }

        currentLine != null
    }

    @Override
    Object next()
    {
        if (hasNext()) inSection = true

        currentLine
    }

    @Override
    void remove()
    {
        throw new UnsupportedOperationException()
    }
}
