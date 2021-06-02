# Shadow CLJS Example Breakage

This is a small, self-contained example of how the dependency resolution policy
of Shadow CLJS can break an app.

## Context

The approach that Node.js uses to resolve dependencies is described in [the
Node
documentation](https://nodejs.org/api/modules.html#modules_loading_from_node_modules_folders).
Essentially, it looks in the `node_modules` directory under the directory
containing the currently evaluated file, then the `node_modules` directory in
the parent directory, and so on up to the root directory.

This approach allows Node.js to have multiple versions of the same package,
even within the same project. For example, say:

- the project has dependencies on packages A and B,
- package A depends on package C at version vX, and
- package B depends on package C at version vY.

Then npm will pick one version of C, say vY, to use as the default version, and
install it under the `node_modules` directory of the project, at
`node_modules/C`. Then, for package A, it will install version vX of C under
the `node_modules` directory of package A, at `node_modules/A/node_modules/C`.
Thus the project as a whole uses A at vX, yet project B uses A at vY, and both
versions can coexist without conflict within the same project.

Shadow CLJS, on the other hand, restricts its attention to the top-level
`node_modules` directory, so never notices that there's a version vY available.
This is a deliberate design decision by Shadow CLJS, as described in
[Issue #547](https://github.com/thheller/shadow-cljs/issues/547). The rationale
is that, in a web context, it makes less sense (and is a considerable source of
complexity) to have different versions of the same package.

## Problem

However, it's not uncommon to need multiple versions of a dependency. We
encountered such a use case with Evergreen UI, which indirectly depended on two
totally incompatible versions of the `inline-style-prefixer` package. Node.js
projects handle this case without a problem. Shadow CLJS projects, on the other
hand, only use a single version, breaking one depending package or the other.

One might argue that this is a flaw with Evergreen, which they should fix.
Maybe so, but practically speaking, given that this isn't a pain anyone using
Node and NPM feels, it's not realistic to expect that all such problems will be
resolved for us in such a way that Shadow's approach will always suffice.

The code demonstrating this problem is in the first commit of this repo. See
[that commit
message](https://github.com/kyptin/shadow-cljs-example-breakage/commit/9ac81921a0bf50057b3af58865e36dad99f59352)
for full details, including a good writeup.

Note that it's only when the versions have changed enough or broken the older
API badly enough that you start to see problems. Using the example above, if
package A's use of package C is actually compatible with version vY of C, then
no problems will manifest even with Shadow CLJS.

And the problems you might see do _not_ point to this issue as the root cause.
All manner of problems and misleading error messages can ensue. Worse, you
probably won't identify this as the root cause without digging into the
implementation of the dependency. And who has time for that?

## Workarounds

Various workarounds exist. One approach is to determine which dependency uses
an old version of the conflicting package. Then, you can upgrade it to use a
newer version.

Once you have a fixed version of the dependency, you need to use it in your
project somehow. The easiest way is to ["vendor" the fixed version in your
repo](https://stackoverflow.com/q/15806241/202292). This approach is
demonstrated in [the second
commit](https://github.com/kyptin/shadow-cljs-example-breakage/commit/710573a7354cd51ab5042cb13ad75093de418725).

You can also share the fix with others, perhaps opening a pull request with the
project, as we've done
[here](https://github.com/threepointone/glamor/pull/396). Once the PR is
accepted and a new version is cut, you can use the new version in your project
and remove your vendored fix. (You may have to wait for multiple projects to
cut new versions if there are several layers of dependencies between your
project and the outdated package.)

## Conclusion

We now understand that, when we encounter a mysterious error in our app that
doesn't seem to be the fault of our code, we may be running into the
constraints imposed by Shadow CLJS's dependency resolution policy. However,
diagnosing whether that is indeed the cause is a tedious proposition that we'd
rather avoid.

So, in our analysis, the convenience provided by Shadow CLJS is not worth the
effort. It is our (as yet unvalidated) hope that Figwheel will not have this
limitation. Perhaps others will have better luck with Shadow and use
dependencies that don't trigger this class of bug.
