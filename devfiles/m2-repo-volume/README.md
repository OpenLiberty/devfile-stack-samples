# m2-repo-volume

Sample devfile for creating and mounting a persistent volume to use as the Maven repository.

### Highlights

* Volume is created and mounted to /home/user/.m2
* Dependencies cached in stack image are copied to /home/user/.m2/repository on the first push (this adds some additional time).
* Subsequent redeploys of the pod see improved startup times since dependencies are not re-downloaded.