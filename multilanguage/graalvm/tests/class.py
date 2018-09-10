class Pythontest:
    def construct(self):
        return Pythontest()

    def __init__(self):
        i = 1234
        s = "snakes"

    def mutate(self):
        i = 2345
        s = "danger noodle"
        return self

Pythontest()
