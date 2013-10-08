'''
    Care about images with help of the amazing (perl) library Exiftool.
'''

import subprocess
import parser
import shutil


class ExiftoolStripper(parser.GenericParser):
    '''
        A generic stripper class using exiftool as backend
    '''

    def __init__(self, filename, parser, mime, backup, **kwargs):
        super(ExiftoolStripper, self).__init__(filename, parser, mime, backup, **kwargs)
        self.allowed = ['ExifTool Version Number', 'File Name', 'Directory',
                'File Size', 'File Modification Date/Time', 'File Access Date/Time', 'File Permissions',
                'File Type', 'MIME Type', 'Image Width', 'Image Height',
                'Image Size']
        self._set_allowed()

    def _set_allowed(self):
        '''
            Set the allowed/harmless list of metadata
        '''
        raise NotImplementedError

    def remove_all(self):
        '''
            Remove all metadata with help of exiftool
        '''
        try:
            if self.backup:
                self.create_backup_copy()
            # Note: '-All=' must be followed by a known exiftool option.
            process = subprocess.Popen( ['exiftool', '-m', '-all=',
                '-adobe=', '-overwrite_original', self.filename ],
                stdout=open('/dev/null'))
            process.wait()
            return True
        except:
            return False

    def is_clean(self):
        '''
            Check if the file is clean with help of exiftool
        '''
        out = subprocess.Popen(['exiftool', self.filename],
                stdout=subprocess.PIPE).communicate()[0]
        out = out.split('\n')
        for i in out[:-1]:
            if i.split(':')[0].strip() not in self.allowed:
                return False
        return True

    def get_meta(self):
        '''
            Return every harmful meta with help of exiftool.
            Exiftool output looks like this:
            field name : value
            field name : value
        '''
        out = subprocess.Popen(['exiftool', self.filename],
                stdout=subprocess.PIPE).communicate()[0]
        out = out.split('\n')
        meta = {}
        for i in out[:-1]:
            key = i.split(':')[0].strip()
            if key not in self.allowed:
                meta[key] = i.split(':')[1].strip()  # add the field name to the metadata set
        return meta


class JpegStripper(ExiftoolStripper):
    '''
        Care about jpeg files with help
        of exiftool
    '''
    def _set_allowed(self):
        self.allowed.extend(['JFIF Version', 'Resolution Unit',
        'X Resolution', 'Y Resolution', 'Encoding Process', 'Bits Per Sample',
        'Color Components', 'Y Cb Cr Sub Sampling'])

class PngStripper(ExiftoolStripper):
    '''
        Care about png files with help
        of exiftool
    '''
    def _set_allowed(self):
        self.allowed.extend(['Bit Depth', 'Color Type', 'Compression',
            'Filter', 'Interlace', 'Pixels Per Unit X', 'Pixels Per Unit Y',
            'Pixel Units', 'Significant Bits' ,'Background Color',
            'SRGB Rendering',])
