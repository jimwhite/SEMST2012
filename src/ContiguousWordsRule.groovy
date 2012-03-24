import groovy.transform.EqualsAndHashCode
import java.util.regex.Pattern
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class ContiguousWordsRule extends Rule
{
    List<String> pos

    ContiguousWordsRule(List<Map> tokens, Cue cue)
    {
        pos = cue.pos
    }

    @Override
    List<Cue> match(List<Map> tokens)
    {
        []
    }
}
