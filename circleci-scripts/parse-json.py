# This python scripts reads a json file and removes the escape sequences like \ or \\ etc.. from it.
import json

with open('secretMgr.json', 'r') as file:
    secString = file.read()

decodedString = bytes(secString[0:],"utf-8").decode("unicode_escape")
print(decodedString)