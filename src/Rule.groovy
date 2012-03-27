abstract class Rule
{
    Cue.CueType type
    
    abstract List<Cue> match(List<Map> tokens);

    void addPositive(List<Map> tokens, Cue cue) { }

    void addNegative(List<Map> tokens, Cue cue) { }

    static Rule ruleForCue(List<Map> instance, Cue cue)
    {
        switch (cue.type) {
            case Cue.CueType.WORD : new WordRule(instance, cue)
                break
            case Cue.CueType.AFFIX : new AffixRule(instance, cue)
                break
            case Cue.CueType.MULTIWORD_CONTIGUOUS : new ContiguousWordsRule(instance, cue)
                break
            case Cue.CueType.MULTIWORD_GAPPY : new GappyWordsRule(instance, cue)
                break
        }
    }
}
