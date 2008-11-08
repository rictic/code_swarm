# ========================================================================
# Copyright (c) 2007, Metaweb Technologies, Inc.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above
#       copyright notice, this list of conditions and the following
#       disclaimer in the documentation and/or other materials provided
#       with the distribution.
# 
# THIS SOFTWARE IS PROVIDED BY METAWEB TECHNOLOGIES AND CONTRIBUTORS
# ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL METAWEB
# TECHNOLOGIES OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# ========================================================================

import string
import re

def quotekey(ustr):
    """
    quote a unicode string to turn it into a valid namespace key
    
    """
    valid_always = string.ascii_letters + string.digits
    valid_interior_only = valid_always + '_-'

    if isinstance(ustr, str):
        s = unicode(ustr,'utf-8')        
    elif isinstance(ustr, unicode):
        s = ustr
    else:
        raise ValueError, 'quotekey() expects utf-8 string or unicode'

    output = []
    if s[0] in valid_always:
        output.append(s[0])
    else:
        output.append('$%04X' % ord(s[0]))

    for c in s[1:-1]:
        if c in valid_interior_only:
            output.append(c)
        else:
            output.append('$%04X' % ord(c))

    if len(s) > 1:
        if s[-1] in valid_always:
            output.append(s[-1])
        else:
            output.append('$%04X' % ord(s[-1]))

    return str(''.join(output))


def unquotekey(key, encoding=None):
    """
    unquote a namespace key and turn it into a unicode string
    """

    valid_always = string.ascii_letters + string.digits

    output = []
    i = 0
    while i < len(key):
        if key[i] in valid_always:
            output.append(key[i])
            i += 1
        elif key[i] in '_-' and i != 0 and i != len(key):
            output.append(key[i])
            i += 1
        elif key[i] == '$' and i+4 < len(key):
            # may raise ValueError if there are invalid characters
            output.append(unichr(int(key[i+1:i+5],16)))
            i += 5
        else:
            raise ValueError, "unquote key saw invalid character '%s' at position %d" % (key[i], i)

    ustr = u''.join(output)
    
    if encoding is None:
        return ustr

    return ustr.encode(encoding)



def id_to_urlid(id):
    """
    convert a mql id to an id suitable for embedding in a url path.
    """

    # XXX shouldn't be in metaweb.api!
    from mw.formats.http import urlencode_pathseg

    segs = id.split('/')

    assert isinstance(id, str) and id != '', 'bad id "%s"' % id

    if id[0] == '~':
        assert len(segs) == 1
        # assume valid, should check
        return id

    if id[0] == '#':
        assert len(segs) == 1
        # assume valid, should check
        return '%23' + id[1:]

    if id[0] != '/':
        raise ValueError, 'unknown id format %s' % id

    # ok, we have a slash-path
    # requote components as keys and rejoin.
    # urlids do not have leading slashes!!!
    return '/'.join(urlencode_pathseg(unquotekey(seg)) for seg in segs[1:])

