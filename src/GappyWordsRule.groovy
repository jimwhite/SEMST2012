import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class GappyWordsRule extends Rule
{
    List<String> pos

    GappyWordsRule(List<Map> tokens, Cue cue)
    {
        pos = cue.pos
    }

    @Override
    List<Cue> match(List<Map> tokens)
    {
        []
    }
}
