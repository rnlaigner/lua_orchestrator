package br.pucrio.inf.les.ese.lua_orchestrator.queue;

public enum TopicStrategy {

    ONE_TOPIC("ONE_TOPIC"),
    ONE_TOPIC_PER_TEN("ONE_TOPIC_PER_TEN");

    // declaring private variable for getting values
    private String name;

    // getter method
    public String getName()
    {
        return this.name;
    }

    // enum constructor - cannot be public or protected
    private TopicStrategy(String name)
    {
        this.name = name;
    }

}
