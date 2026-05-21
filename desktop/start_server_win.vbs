' StateLink — Silent launcher for Windows auto-start
' This VBS script runs the Python server without showing a console window.
' It is placed in the Windows Startup folder by setup_autostart.py.

' Paths — these are replaced by setup_autostart.py with absolute paths
' or you can edit them manually below.
ServerDir = "C:\StateLink\desktop"
PythonExe = "pythonw.exe"
ServerScript = "statelink_server.py"
LogFile = ServerDir & "\statelink_server.log"

' Build command
Cmd = """" & PythonExe & """ """ & ServerScript & """"

' Create shell object and run
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Change to server directory
WshShell.CurrentDirectory = ServerDir

' Run silently (window style 0 = hidden, False = don't wait)
WshShell.Run Cmd, 0, False

Set WshShell = Nothing
Set fso = Nothing
